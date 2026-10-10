import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HouseholdService } from '../household/household.service';
import { SnackbarService } from '../shared/snackbar.service';
import { householdStub, storeStub, task } from '../testing';
import { TaskStore } from './task.store';
import { TasksPage } from './tasks-page';

describe('TasksPage', () => {
  function render(store: ReturnType<typeof storeStub>) {
    TestBed.configureTestingModule({
      imports: [TasksPage],
      providers: [
        provideRouter([]),
        { provide: TaskStore, useValue: store },
        { provide: HouseholdService, useValue: householdStub() },
      ],
    });
    const fixture = TestBed.createComponent(TasksPage);
    fixture.detectChanges();
    return fixture;
  }

  it('marks a task done and offers undo in the snackbar', async () => {
    const store = storeStub([task()]);
    const fixture = render(store);
    const snackbar = TestBed.inject(SnackbarService);

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.mine')!.click();

    expect(store.markDone).toHaveBeenCalledWith(1, 'alex');
    expect(snackbar.snack()?.text).toBe('Erledigt von Alex');
    await snackbar.snack()!.undo!();
    expect(store.undo).toHaveBeenCalledWith(42);
  });

  it('shows the empty state without tasks', () => {
    const fixture = render(storeStub([]));

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Noch keine Aufgaben');
  });
});
