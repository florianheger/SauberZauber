import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { Login } from './login/login';
import { Shell } from './shell/shell';
import { StatsPage } from './stats/stats-page';
import { TasksPage } from './tasks/tasks-page';

export const routes: Routes = [
  { path: 'login', component: Login, title: 'Anmelden – SauberZauber' },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', component: TasksPage, title: 'Aufgaben – SauberZauber' },
      { path: 'statistik', component: StatsPage, title: 'Statistik & Aufgaben – SauberZauber' },
    ],
  },
  { path: '**', redirectTo: '' },
];
