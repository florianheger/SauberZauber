import { TestBed } from '@angular/core/testing';
import { HouseholdService } from '../household/household.service';
import { ALEX, householdStub, SAM, task } from '../testing';
import { TaskCard } from './task-card';

describe('TaskCard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TaskCard],
      providers: [{ provide: HouseholdService, useValue: householdStub() }],
    });
  });

  function render(overrides = {}) {
    const fixture = TestBed.createComponent(TaskCard);
    fixture.componentRef.setInput('task', task(overrides));
    fixture.componentRef.setInput('me', ALEX);
    fixture.componentRef.setInput('other', SAM);
    fixture.detectChanges();
    return fixture;
  }

  it('marks the task done for me with the check button', () => {
    const fixture = render();
    const done = vi.fn();
    fixture.componentInstance.done.subscribe(done);

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.mine')!.click();

    expect(done).toHaveBeenCalledWith(ALEX);
  });

  it('marks the task done for the other person with the initial button', () => {
    const fixture = render();
    const done = vi.fn();
    fixture.componentInstance.done.subscribe(done);
    const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.other')!;

    button.click();

    expect(button.textContent?.trim()).toBe('S');
    expect(button.getAttribute('aria-label')).toBe('Von Sam erledigt');
    expect(done).toHaveBeenCalledWith(SAM);
  });

  it('shows who did it last', () => {
    const fixture = render({ lastDone: { username: 'sam', at: new Date().toISOString() } });

    expect((fixture.nativeElement as HTMLElement).querySelector('.last')?.textContent).toContain(
      'Zuletzt Sam · gerade eben',
    );
  });

  it('opens the chooser on right-click', () => {
    const fixture = render();
    const longPress = vi.fn();
    fixture.componentInstance.longPress.subscribe(longPress);

    (fixture.nativeElement as HTMLElement).querySelector('.card')!.dispatchEvent(new MouseEvent('contextmenu'));

    expect(longPress).toHaveBeenCalled();
  });
});
