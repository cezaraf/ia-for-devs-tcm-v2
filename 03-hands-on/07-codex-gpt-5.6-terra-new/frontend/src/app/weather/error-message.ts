import { Component, input, output } from '@angular/core';
@Component({selector:'app-error-message',templateUrl:'./error-message.html',styleUrl:'./error-message.css'}) export class ErrorMessage { message=input.required<string>(); retryable=input(false); retry=output<void>(); }
