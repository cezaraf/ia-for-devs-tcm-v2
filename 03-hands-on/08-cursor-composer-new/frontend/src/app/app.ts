import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { WeatherPanel } from './weather/weather-panel';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, WeatherPanel],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
