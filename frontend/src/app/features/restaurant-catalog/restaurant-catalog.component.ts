import { Component, EventEmitter, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { Restaurant, MenuItem } from '../../core/models/restaurant.model';

@Component({
  selector: 'app-restaurant-catalog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './restaurant-catalog.component.html',
  styleUrls: ['./restaurant-catalog.component.css']
})
export class RestaurantCatalogComponent implements OnInit {
  private api = inject(ApiService);

  restaurants = signal<Restaurant[]>([]);
  selectedRestaurant = signal<Restaurant | null>(null);
  menuItems = signal<MenuItem[]>([]);
  loadingMenu = signal<boolean>(false);

  @Output() addItem = new EventEmitter<{ restaurant: Restaurant; item: MenuItem }>();

  ngOnInit() {
    this.api.getRestaurants().subscribe({
      next: (list) => {
        this.restaurants.set(list);
        if (list.length > 0) {
          this.selectRestaurant(list[0]);
        }
      }
    });
  }

  selectRestaurant(r: Restaurant) {
    this.selectedRestaurant.set(r);
    this.loadingMenu.set(true);
    this.api.getMenuItems(r.id).subscribe({
      next: (items) => {
        this.menuItems.set(items);
        this.loadingMenu.set(false);
      },
      error: () => this.loadingMenu.set(false)
    });
  }

  onAddDish(item: MenuItem) {
    const r = this.selectedRestaurant();
    if (r) {
      this.addItem.emit({ restaurant: r, item });
    }
  }
}
