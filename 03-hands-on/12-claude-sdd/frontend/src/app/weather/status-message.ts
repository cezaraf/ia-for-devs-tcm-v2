import { Component, input, output } from '@angular/core';
import { RETRY_LABEL } from './weather-messages';

@Component({
  selector: 'app-status-message',
  imports: [],
  templateUrl: './status-message.html',
  styleUrl: './status-message.css',
})
export class StatusMessage {
  readonly message = input.required<string>();
  readonly kind = input.required<'info' | 'error'>();
  readonly retryable = input(false);
  readonly retry = output<void>();

  protected readonly retryLabel = RETRY_LABEL;
}
