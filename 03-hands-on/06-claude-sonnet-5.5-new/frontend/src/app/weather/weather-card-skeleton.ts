import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-weather-card-skeleton',
  templateUrl: './weather-card-skeleton.html',
  styleUrl: './weather-card-skeleton.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeatherCardSkeleton {
  protected readonly rows = ['apparent', 'humidity', 'wind', 'precipitation'];
}
