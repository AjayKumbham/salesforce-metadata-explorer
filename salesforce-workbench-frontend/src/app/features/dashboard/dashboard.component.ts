import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  private authService = inject(AuthService);
  
  user = this.authService.currentUser;

  get cleanInstanceUrl(): string {
    const url = this.user()?.instanceUrl;
    if (!url) return '—';
    try {
      const parsedUrl = new URL(url);
      return parsedUrl.origin;
    } catch {
      return url.split('/services')[0];
    }
  }

  logout() {
    this.authService.logout();
  }
}
