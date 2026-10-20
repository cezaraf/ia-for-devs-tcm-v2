import { Component } from '@angular/core';
import { LOADING_TEXT } from './weather-messages';

@Component({
  selector: 'app-weather-skeleton',
  imports: [],
  templateUrl: './weather-skeleton.html',
  styleUrl: './weather-skeleton.css',
})
export class WeatherSkeleton {
  protected readonly loadingText = LOADING_TEXT;
}
