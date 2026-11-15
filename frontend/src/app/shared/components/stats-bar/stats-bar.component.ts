import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stats-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stats-bar.component.html',
  styleUrls: ['./stats-bar.component.css']
})
export class StatsBarComponent {
  @Input() restaurantCount = 5;
  @Input() menuItemCount = 50;
  @Input() activeAgentCount = 20;
  @Input() activeOrdersCount = 0;
}
