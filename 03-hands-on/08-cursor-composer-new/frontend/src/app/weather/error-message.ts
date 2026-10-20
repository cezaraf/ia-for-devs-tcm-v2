import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-error-message',
  templateUrl: './error-message.html',
  styleUrl: './error-message.css',
})
export class ErrorMessage {
  readonly message = input.required<string>();
  readonly showRetry = input(false);
  readonly retry = output<void>();
}
