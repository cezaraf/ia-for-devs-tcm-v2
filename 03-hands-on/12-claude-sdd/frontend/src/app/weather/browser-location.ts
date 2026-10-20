import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable } from 'rxjs';

export interface Position {
  latitude: number;
  longitude: number;
}

@Injectable({ providedIn: 'root' })
export class BrowserLocation {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  isAvailable(): boolean {
    return this.isBrowser && 'geolocation' in navigator;
  }

  getPosition(): Observable<Position> {
    return new Observable<Position>((subscriber) => {
      let closed = false;
      navigator.geolocation.getCurrentPosition(
        (position) => {
          if (closed) {
            return;
          }
          subscriber.next({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
          subscriber.complete();
        },
        (error) => {
          if (closed) {
            return;
          }
          subscriber.error({ code: error.code });
        },
        { enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 },
      );
      return () => {
        closed = true;
      };
    });
  }
}
