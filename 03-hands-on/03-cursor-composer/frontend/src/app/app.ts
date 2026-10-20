import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { WeatherPanelComponent } from './weather/weather-panel.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, WeatherPanelComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
