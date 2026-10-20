import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-error-message',
  imports: [],
  templateUrl: './error-message.html',
  styleUrl: './error-message.css'
})
export class ErrorMessage {
  readonly message = input.required<string>();
  readonly canRetry = input<boolean>(false);
  readonly retry = output<void>();

  onRetryClick(): void {
    this.retry.emit();
  }
}
