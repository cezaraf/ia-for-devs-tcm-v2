import { Observable } from 'rxjs';

export interface Coordinates {
  latitude: number;
  longitude: number;
}

export class GeolocationFailure extends Error {
  constructor(readonly denied: boolean) {
    super(denied ? 'geolocation denied' : 'geolocation unavailable');
  }
}

const PERMISSION_DENIED = 1;

/** Só chamar no navegador. Cancelar a inscrição descarta o resultado tardio. */
export function currentPosition(): Observable<Coordinates> {
  return new Observable<Coordinates>((subscriber) => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      subscriber.error(new GeolocationFailure(false));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        subscriber.next({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        });
        subscriber.complete();
      },
      (error) => subscriber.error(new GeolocationFailure(error.code === PERMISSION_DENIED)),
      { timeout: 10000, maximumAge: 600000 },
    );
  });
}
