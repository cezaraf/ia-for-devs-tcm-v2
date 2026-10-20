import { Component, input, output } from '@angular/core';

/** Mensagem de erro com marcador [x] e botão "tentar novamente" quando repetível. */
@Component({
  selector: 'app-error-message',
  templateUrl: './error-message.html',
  styleUrl: './error-message.css',
})
export class ErrorMessage {
  readonly message = input.required<string>();
  readonly retryable = input(false);

  readonly retry = output<void>();
}
