import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  imports: [],
  templateUrl: './oauth-callback.component.html',
  styleUrl: './oauth-callback.component.css'
})
export class OauthCallbackComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  
  statusMessage = signal('Authenticating with Salesforce...');

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const connectionId = params['connectionId'];
      
      if (connectionId) {
        this.statusMessage.set('Validating connection...');
        
        this.authService.setSessionFromOAuth(connectionId).subscribe(success => {
          if (success) {
            this.router.navigate(['/dashboard']);
          } else {
            this.statusMessage.set('Failed to validate connection. Please try logging in again.');
            setTimeout(() => this.router.navigate(['/login']), 3000);
          }
        });
      } else {
        this.statusMessage.set('No connection ID provided. Returning to login...');
        setTimeout(() => this.router.navigate(['/login']), 3000);
      }
    });
  }
}
