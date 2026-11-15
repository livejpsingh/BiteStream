import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { OtpVerificationResponse } from '../../../core/models/delivery.model';

@Component({
  selector: 'app-otp-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './otp-modal.component.html',
  styleUrls: ['./otp-modal.component.css']
})
export class OtpModalComponent {
  private api = inject(ApiService);

  @Input() isOpen = false;
  @Input() orderId = 0;
  @Input() orderNumber = '';
  @Input() stepType: 'PICKUP' | 'DELIVERY' = 'PICKUP';
  @Input() activeOtpHint = '';

  @Output() close = new EventEmitter<void>();
  @Output() otpConfirmed = new EventEmitter<OtpVerificationResponse>();

  enteredOtp = '';
  loading = false;
  errorMessage = '';

  quickFill() {
    if (this.activeOtpHint && this.activeOtpHint !== 'EXPIRED_OR_NOT_ISSUED') {
      this.enteredOtp = this.activeOtpHint;
    }
  }

  submitOtp() {
    if (!this.enteredOtp || this.enteredOtp.trim().length < 6) {
      this.errorMessage = 'Please enter a valid 6-digit numeric OTP.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const action$ = this.stepType === 'PICKUP'
      ? this.api.confirmPickup(this.orderId, this.enteredOtp.trim())
      : this.api.confirmDelivery(this.orderId, this.enteredOtp.trim());

    action$.subscribe({
      next: (res) => {
        this.loading = false;
        this.otpConfirmed.emit(res);
        this.closeModal();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'OTP verification failed. Please try again.';
      }
    });
  }

  closeModal() {
    this.enteredOtp = '';
    this.errorMessage = '';
    this.close.emit();
  }
}
