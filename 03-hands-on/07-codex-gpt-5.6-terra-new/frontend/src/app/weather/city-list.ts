import { Component, input, output } from '@angular/core'; import { City } from './weather-api';
@Component({selector:'app-city-list',templateUrl:'./city-list.html',styleUrl:'./city-list.css'}) export class CityList { cities=input.required<City[]>(); selected=output<City>(); label(c:City){return ['[+] '+c.name,c.admin1,c.country].filter(Boolean).join(' — ');} }
