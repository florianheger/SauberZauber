import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface Task {
  id: number;
  title: string;
  description: string | null;
  icon: string;
  /** How often each user did the task, by username. */
  counts: Record<string, number>;
  lastDone: { username: string; at: string } | null;
}

export interface TaskInput {
  title: string;
  description: string | null;
  icon: string;
  /** On update: corrected completion counts by username (omit to leave them unchanged). */
  counts?: Record<string, number>;
}

interface Completion {
  id: number;
  username: string;
  at: string;
}

/** Tasks and their statistics; every change reloads the list from the backend. */
@Injectable({ providedIn: 'root' })
export class TaskStore {
  private readonly http = inject(HttpClient);

  /** null until loaded */
  readonly tasks = signal<Task[] | null>(null);

  async load(): Promise<void> {
    this.tasks.set(await firstValueFrom(this.http.get<Task[]>('/api/tasks')));
  }

  async create(input: TaskInput): Promise<void> {
    await firstValueFrom(this.http.post('/api/tasks', input));
    await this.load();
  }

  async update(id: number, input: TaskInput): Promise<void> {
    await firstValueFrom(this.http.put(`/api/tasks/${id}`, input));
    await this.load();
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.http.delete(`/api/tasks/${id}`));
    await this.load();
  }

  /** Records that the user did the task; returns the completion id (for undo). */
  async markDone(taskId: number, username: string): Promise<number> {
    const completion = await firstValueFrom(
      this.http.post<Completion>(`/api/tasks/${taskId}/completions`, { username }),
    );
    await this.load();
    return completion.id;
  }

  async undo(completionId: number): Promise<void> {
    await firstValueFrom(this.http.delete(`/api/completions/${completionId}`));
    await this.load();
  }
}
