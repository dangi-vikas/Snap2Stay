import clsx, { type ClassValue } from 'clsx';

/**
 * Conditional className helper. Single source of truth so we never sprinkle
 * raw template strings across components.
 */
export const cn = (...inputs: ClassValue[]): string => clsx(inputs);
