export interface LocationInfo {
  name: string;
  state?: string;
  country?: string;
  latitude: number;
  longitude: number;
  timezone: string;
}

export interface CurrentWeatherInfo {
  temperature: number;
  apparentTemperature: number;
  humidity: number;
  precipitation: number;
  windSpeed: number;
  windDirection: number;
  cloudCover: number;
  weatherCode: number;
  condition: string;
  icon: string;
  isDay: boolean;
  observedAt: string;
}

export interface DailyForecastInfo {
  date: string;
  maxTemp: number;
  minTemp: number;
  weatherCode: number;
  condition: string;
  icon: string;
}

export interface WeatherResponse {
  location: LocationInfo;
  current: CurrentWeatherInfo;
  dailyForecast: DailyForecastInfo[];
}
