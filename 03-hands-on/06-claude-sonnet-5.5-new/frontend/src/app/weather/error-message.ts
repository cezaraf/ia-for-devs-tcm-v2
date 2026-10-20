import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-error-message',
  templateUrl: './error-message.html',
  styleUrl: './error-message.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorMessage {
  readonly message = input.required<string>();
  readonly retryable = input(false);
  readonly retry = output<void>();
}
