/**
 * Authentication & user identity types (auth-service).
 */

export type UserRole = 'USER' | 'ADMIN';

export interface User {
  id: string;
  fullName: string;
  email: string;
  role: UserRole;
  createdAt: string;
  updatedAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
}

/** React Hook Form values for the login form. */
export interface LoginFormValues {
  email: string;
  password: string;
  rememberMe?: boolean;
}

/** React Hook Form values for the registration form. */
export interface RegisterFormValues {
  fullName: string;
  email: string;
  password: string;
  confirmPassword: string;
}
