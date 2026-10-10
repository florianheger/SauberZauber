import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

interface HelloResponse {
  message: string;
}

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly message = signal('Lade …');

  ngOnInit(): void {
    this.http.get<HelloResponse>('/api/hello').subscribe({
      next: (response) => this.message.set(response.message),
      error: () => this.message.set('Backend nicht erreichbar'),
    });
  }
}
