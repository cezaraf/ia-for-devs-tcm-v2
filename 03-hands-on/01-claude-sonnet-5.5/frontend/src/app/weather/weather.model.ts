export interface WeatherResponse {
  location: {
    name: string | null;
    region: string | null;
    country: string | null;
    latitude: number;
    longitude: number;
    timezone: string | null;
  };
  current: {
    time: string;
    temperature: number;
    apparentTemperature: number;
    humidity: number;
    precipitation: number;
    windSpeed: number;
    windDirection: number;
    day: boolean;
    weatherCode: number;
    description: string;
    icon: string;
  };
  units: {
    temperature: string;
    humidity: string;
    precipitation: string;
    windSpeed: string;
  };
}
