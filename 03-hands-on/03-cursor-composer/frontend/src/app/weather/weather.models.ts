export interface WeatherData {
  city: string;
  country: string | null;
  admin1: string | null;
  latitude: number;
  longitude: number;
  temperatureCelsius: number;
  relativeHumidityPercent: number;
  windSpeedKmh: number;
  weatherCode: number;
  weatherDescription: string;
  observedAt: string;
}
