import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { SwPush } from '@angular/service-worker';
import { firstValueFrom } from 'rxjs';

/**
 * - `unavailable`: not supported here (no service worker, old browser, or disabled on the server)
 * - `install-needed`: iOS only delivers push to the app added to the home screen
 * - `denied`: blocked in the browser or system settings
 */
export type PushState = 'unavailable' | 'install-needed' | 'denied' | 'off' | 'on';

/** Push notifications for this device: permission, browser subscription and its registration in the backend. */
@Injectable({ providedIn: 'root' })
export class PushService {
  private readonly http = inject(HttpClient);
  private readonly swPush = inject(SwPush, { optional: true });

  readonly state = signal<PushState>('unavailable');
  readonly busy = signal(false);
  private publicKey: string | null = null;

  /** Messages pushed while the app is open. */
  get messages() {
    return this.swPush?.messages;
  }

  /** Determines the state and re-registers an existing subscription (endpoints can change, or the user did). */
  async init(): Promise<void> {
    try {
      this.publicKey = (await firstValueFrom(this.http.get<{ publicKey: string }>('/api/push/public-key'))).publicKey;
    } catch {
      return this.state.set('unavailable');
    }
    if (isIos() && !isStandalone()) {
      return this.state.set('install-needed');
    }
    if (!this.swPush?.isEnabled || !('Notification' in window) || !('PushManager' in window)) {
      return this.state.set('unavailable');
    }
    if (Notification.permission === 'denied') {
      return this.state.set('denied');
    }
    const subscription = await firstValueFrom(this.swPush.subscription);
    if (!subscription || Notification.permission !== 'granted') {
      return this.state.set('off');
    }
    if (!hasKey(subscription, this.publicKey)) {
      // the server's VAPID key changed; the old subscription can no longer be used
      await this.swPush.unsubscribe();
      return this.state.set('off');
    }
    await this.register(subscription);
    this.state.set('on');
  }

  /** Must be called directly from a tap/click handler: iOS and Firefox only ask for permission then. */
  async enable(): Promise<void> {
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      this.state.set(permission === 'denied' ? 'denied' : 'off');
      return;
    }
    this.busy.set(true);
    try {
      const subscription = await this.swPush!.requestSubscription({ serverPublicKey: this.publicKey! });
      await this.register(subscription);
      this.state.set('on');
    } finally {
      this.busy.set(false);
    }
  }

  async disable(): Promise<void> {
    this.busy.set(true);
    try {
      const subscription = await firstValueFrom(this.swPush!.subscription);
      if (subscription) {
        await firstValueFrom(
          this.http.delete('/api/push/subscriptions', { body: { endpoint: subscription.endpoint } }),
        );
        await this.swPush!.unsubscribe();
      }
      this.state.set('off');
    } finally {
      this.busy.set(false);
    }
  }

  private register(subscription: PushSubscription): Promise<unknown> {
    return firstValueFrom(this.http.post('/api/push/subscriptions', subscription.toJSON()));
  }
}

function isIos(): boolean {
  return (
    /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
  );
}

function isStandalone(): boolean {
  return (
    (navigator as Navigator & { standalone?: boolean }).standalone === true ||
    window.matchMedia?.('(display-mode: standalone)').matches
  );
}

function hasKey(subscription: PushSubscription, publicKey: string): boolean {
  const key = subscription.options?.applicationServerKey;
  if (!key) {
    return true;
  }
  const bytes = new Uint8Array(key);
  const encoded = btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return encoded === publicKey.replace(/=+$/, '');
}
