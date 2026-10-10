import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HouseholdService } from '../household/household.service';
import { TaskStore } from '../tasks/task.store';
import { householdStub, storeStub, task } from '../testing';
import { StatsPage } from './stats-page';

describe('StatsPage', () => {
  it('shows totals, per-task counts and the fairness text', () => {
    TestBed.configureTestingModule({
      imports: [StatsPage],
      providers: [
        provideRouter([]),
        {
          provide: TaskStore,
          useValue: storeStub([
            task({ id: 1, title: 'Müll rausbringen', counts: { alex: 1, sam: 10 } }),
            task({ id: 2, title: 'Staubsaugen', icon: 'cleaning_services', counts: { alex: 4, sam: 5 } }),
          ]),
        },
        { provide: HouseholdService, useValue: householdStub() },
      ],
    });
    const fixture = TestBed.createComponent(StatsPage);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    const counts = [...element.querySelectorAll('.summary-count')].map((e) => e.textContent?.trim());
    expect(counts).toEqual(['5', '15']);
    expect(element.querySelector('.summary-name')?.textContent).toContain('Alex (du)');
    expect(element.querySelector('.fairness')?.textContent).toContain('Wie wär’s als Nächstes mit „Müll rausbringen“?');
    const firstRow = [...element.querySelectorAll('.row')[0].querySelectorAll('.n')].map((e) => e.textContent);
    expect(firstRow).toEqual(['1', '10']);
  });
});
