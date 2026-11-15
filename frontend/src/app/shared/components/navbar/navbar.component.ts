import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  private api = inject(ApiService);
  backendStatus = signal<'UP' | 'OFFLINE' | 'CHECKING'>('CHECKING');

  ngOnInit() {
    this.checkHealth();
  }

  checkHealth() {
    this.api.checkHealth().subscribe({
      next: (res) => this.backendStatus.set(res.status.includes('UP') ? 'UP' : 'OFFLINE'),
      error: () => this.backendStatus.set('OFFLINE')
    });
  }
}
