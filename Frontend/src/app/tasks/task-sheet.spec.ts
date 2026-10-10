import { TestBed } from '@angular/core/testing';
import { storeStub, task } from '../testing';
import { TaskSheet } from './task-sheet';
import { TaskStore } from './task.store';

describe('TaskSheet', () => {
  let store: ReturnType<typeof storeStub>;

  function render(editing: ReturnType<typeof task> | null = null) {
    store = storeStub();
    TestBed.configureTestingModule({ imports: [TaskSheet], providers: [{ provide: TaskStore, useValue: store }] });
    const fixture = TestBed.createComponent(TaskSheet);
    fixture.componentRef.setInput('open', true);
    fixture.componentRef.setInput('task', editing);
    fixture.detectChanges();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  function type(element: HTMLElement, selector: string, value: string) {
    const input = element.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  it('requires a title', async () => {
    const { fixture, element } = render();

    element.querySelector<HTMLButtonElement>('button.save')!.click();
    await fixture.whenStable();

    expect(element.querySelector('.error')?.textContent).toContain('Bitte gib einen Titel ein.');
    expect(store.create).not.toHaveBeenCalled();
  });

  it('creates a task with the icon found by a German search', async () => {
    const { fixture, element } = render();
    type(element, 'input[name=title]', '  Wäsche waschen ');
    type(element, 'input[name=iconSearch]', 'Wäsche');
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('.grid button[title=local_laundry_service]')!.click();

    element.querySelector<HTMLButtonElement>('button.save')!.click();
    await fixture.whenStable();

    expect(store.create).toHaveBeenCalledWith({
      title: 'Wäsche waschen',
      description: null,
      icon: 'local_laundry_service',
    });
  });

  it('deletes a task after confirmation', async () => {
    const { fixture, element } = render(task({ counts: { alex: 2, sam: 1 } }));

    element.querySelector<HTMLButtonElement>('button.delete')!.click();
    fixture.detectChanges();
    expect(element.querySelector('.confirm')?.textContent).toContain('(3× erledigt)');
    element.querySelector<HTMLButtonElement>('.confirm button.danger')!.click();
    await fixture.whenStable();

    expect(store.remove).toHaveBeenCalledWith(1);
  });
});
