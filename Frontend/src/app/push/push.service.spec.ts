import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SwPush } from '@angular/service-worker';
import { of } from 'rxjs';
import { PushService } from './push.service';

describe('PushService', () => {
  const PUBLIC_KEY = 'BFTZ';
  const SUBSCRIPTION_JSON = { endpoint: 'https://push.example.com/1', keys: { p256dh: 'p', auth: 'a' } };

  function fakeSubscription() {
    return { endpoint: SUBSCRIPTION_JSON.endpoint, options: {}, toJSON: () => SUBSCRIPTION_JSON };
  }

  function setUp(options: { subscription?: unknown; permission?: NotificationPermission } = {}) {
    const permission = { value: options.permission ?? 'default' };
    const notification = {
      get permission() {
        return permission.value;
      },
      requestPermission: vi.fn(async () => (permission.value = 'granted')),
    };
    vi.stubGlobal('Notification', notification);
    vi.stubGlobal('PushManager', class {});
    const swPush = {
      isEnabled: true,
      subscription: of(options.subscription ?? null),
      messages: of(),
      requestSubscription: vi.fn(async () => fakeSubscription()),
      unsubscribe: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: SwPush, useValue: swPush }],
    });
    return {
      push: TestBed.inject(PushService),
      http: TestBed.inject(HttpTestingController),
      swPush,
      notification,
    };
  }

  async function init(push: PushService, http: HttpTestingController) {
    const done = push.init();
    http.expectOne('/api/push/public-key').flush({ publicKey: PUBLIC_KEY });
    await Promise.resolve();
    return done;
  }

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('is off until the user turns it on', async () => {
    const { push, http } = setUp();

    await init(push, http);

    expect(push.state()).toBe('off');
  });

  it('asks for permission first, then subscribes and registers the subscription', async () => {
    const { push, http, swPush, notification } = setUp();
    await init(push, http);

    const done = push.enable();
    expect(notification.requestPermission).toHaveBeenCalled();
    await vi.waitFor(() => http.expectOne('/api/push/subscriptions').flush(null));
    await done;

    expect(swPush.requestSubscription).toHaveBeenCalledWith({ serverPublicKey: PUBLIC_KEY });
    expect(push.state()).toBe('on');
  });

  it('re-registers an existing subscription on start', async () => {
    const { push, http } = setUp({ subscription: fakeSubscription(), permission: 'granted' });

    const done = init(push, http);
    await vi.waitFor(() => {
      const request = http.expectOne('/api/push/subscriptions');
      expect(request.request.body).toEqual(SUBSCRIPTION_JSON);
      request.flush(null);
    });
    await done;

    expect(push.state()).toBe('on');
  });

  it('removes the subscription from the backend and the browser', async () => {
    const { push, http, swPush } = setUp({ subscription: fakeSubscription(), permission: 'granted' });

    const done = push.disable();
    await vi.waitFor(() => {
      const request = http.expectOne('/api/push/subscriptions');
      expect(request.request.method).toBe('DELETE');
      expect(request.request.body).toEqual({ endpoint: SUBSCRIPTION_JSON.endpoint });
      request.flush(null);
    });
    await done;

    expect(swPush.unsubscribe).toHaveBeenCalled();
    expect(push.state()).toBe('off');
  });

  it('shows when notifications are blocked', async () => {
    const { push, http } = setUp({ permission: 'denied' });

    await init(push, http);

    expect(push.state()).toBe('denied');
  });

  it('is unavailable when the server has no push keys', async () => {
    const { push, http } = setUp();

    const done = push.init();
    http.expectOne('/api/push/public-key').flush(null, { status: 404, statusText: 'Not Found' });
    await done;

    expect(push.state()).toBe('unavailable');
  });

  it('asks iPhone users to install the app first', async () => {
    const { push, http } = setUp();
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue(
      'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile Safari/604.1',
    );

    await init(push, http);

    expect(push.state()).toBe('install-needed');
  });
});
