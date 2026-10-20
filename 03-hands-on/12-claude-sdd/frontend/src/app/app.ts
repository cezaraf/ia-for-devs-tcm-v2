import { Component } from '@angular/core';
import { WeatherPanel } from './weather/weather-panel';

@Component({
  selector: 'app-root',
  imports: [WeatherPanel],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}
