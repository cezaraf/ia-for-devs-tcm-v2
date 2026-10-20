import { Component } from '@angular/core';

@Component({
  selector: 'app-weather-card-skeleton',
  imports: [],
  templateUrl: './weather-card-skeleton.html',
  styleUrl: './weather-card-skeleton.css',
})
export class WeatherCardSkeleton {
  protected readonly rows = [1, 2, 3, 4];
}
