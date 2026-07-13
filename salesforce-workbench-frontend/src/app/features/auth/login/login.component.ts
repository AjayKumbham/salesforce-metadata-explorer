import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loginForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
    securityToken: [''],
    environment: ['Production', Validators.required],
    apiVersion: ['60.0', Validators.required]
  });

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  apiVersions = ['56.0', '57.0', '58.0', '59.0', '60.0', '61.0', '62.0', '63.0', '64.0'];

  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set(null);
      
      this.authService.login(this.loginForm.value).subscribe(success => {
        this.isLoading.set(false);
        if (success) {
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMessage.set('Authentication failed. Please check your username and password.');
        }
      });
    }
  }

  loginWithOAuth() {
    const env = this.loginForm.get('environment')?.value;
    const apiVer = this.loginForm.get('apiVersion')?.value;
    window.location.href = `http://localhost:8080/api/auth/oauth/login?environment=${env}&apiVersion=${apiVer}`;
  }
}
