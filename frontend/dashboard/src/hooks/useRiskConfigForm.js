import { useState, useCallback, useEffect } from 'react';

/**
 * useRiskConfigForm Custom Hook
 * Manages form state, validation, and submission for risk configuration forms
 * 
 * @param {Object} initialValues - Initial form values
 * @param {Function} onSubmit - Callback on form submission
 * @param {Function} validate - Form validation function
 * @param {Object} options - Additional options
 * @returns {Object} Form state and methods
 */
export const useRiskConfigForm = (
  initialValues = {},
  onSubmit = null,
  validate = null,
  options = {}
) => {
  const { validateOnChange = true, validateOnBlur = true } = options;

  // Form state
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDirty, setIsDirty] = useState(false);

  /**
   * Validate form
   */
  const validateForm = useCallback(
    (valuesToValidate = values) => {
      if (!validate) return {};

      try {
        const validationErrors = validate(valuesToValidate);
        return validationErrors || {};
      } catch (err) {
        console.error('Validation error:', err);
        return {};
      }
    },
    [validate, values]
  );

  /**
   * Handle field change
   */
  const handleChange = useCallback(
    (e) => {
      const { name, value, type, checked } = e.target;
      const fieldValue = type === 'checkbox' ? checked : value;

      setValues((prev) => ({
        ...prev,
        [name]: fieldValue,
      }));

      setIsDirty(true);

      // Validate on change if enabled
      if (validateOnChange) {
        const validationErrors = validateForm({
          ...values,
          [name]: fieldValue,
        });

        setErrors((prev) => ({
          ...prev,
          [name]: validationErrors[name] || null,
        }));
      }
    },
    [values, validateOnChange, validateForm]
  );

  /**
   * Handle field blur
   */
  const handleBlur = useCallback(
    (e) => {
      const { name } = e.target;

      setTouched((prev) => ({
        ...prev,
        [name]: true,
      }));

      // Validate on blur if enabled
      if (validateOnBlur) {
        const validationErrors = validateForm(values);

        setErrors((prev) => ({
          ...prev,
          [name]: validationErrors[name] || null,
        }));
      }
    },
    [values, validateOnBlur, validateForm]
  );

  /**
   * Set field value programmatically
   */
  const setFieldValue = useCallback((name, value) => {
    setValues((prev) => ({
      ...prev,
      [name]: value,
    }));
    setIsDirty(true);

    if (validateOnChange) {
      const validationErrors = validateForm({
        ...values,
        [name]: value,
      });

      setErrors((prev) => ({
        ...prev,
        [name]: validationErrors[name] || null,
      }));
    }
  }, [values, validateOnChange, validateForm]);

  /**
   * Set field error
   */
  const setFieldError = useCallback((name, error) => {
    setErrors((prev) => ({
      ...prev,
      [name]: error,
    }));
  }, []);

  /**
   * Set field touched
   */
  const setFieldTouched = useCallback((name, isTouched = true) => {
    setTouched((prev) => ({
      ...prev,
      [name]: isTouched,
    }));
  }, []);

  /**
   * Handle form submit
   */
  const handleSubmit = useCallback(
    async (e) => {
      if (e && e.preventDefault) {
        e.preventDefault();
      }

      // Mark all fields as touched
      const allTouched = Object.keys(values).reduce(
        (acc, key) => ({ ...acc, [key]: true }),
        {}
      );
      setTouched(allTouched);

      // Validate entire form
      const validationErrors = validateForm(values);
      setErrors(validationErrors);

      // Check if has errors
      if (Object.keys(validationErrors).length > 0) {
        return false;
      }

      if (!onSubmit) return true;

      try {
        setIsSubmitting(true);
        await onSubmit(values);
        return true;
      } catch (err) {
        console.error('Submit error:', err);
        setErrors((prev) => ({
          ...prev,
          _submit: err.message || 'Failed to submit form',
        }));
        return false;
      } finally {
        setIsSubmitting(false);
      }
    },
    [values, validateForm, onSubmit]
  );

  /**
   * Reset form
   */
  const resetForm = useCallback((newValues = null) => {
    const resetValues = newValues || initialValues;
    setValues(resetValues);
    setErrors({});
    setTouched({});
    setIsSubmitting(false);
    setIsDirty(false);
  }, [initialValues]);

  /**
   * Get field props
   */
  const getFieldProps = useCallback(
    (name) => ({
      name,
      value: values[name] || '',
      onChange: handleChange,
      onBlur: handleBlur,
      error: touched[name] ? errors[name] : null,
    }),
    [values, errors, touched, handleChange, handleBlur]
  );

  /**
   * Get field meta
   */
  const getFieldMeta = useCallback(
    (name) => ({
      value: values[name] || '',
      error: errors[name] || null,
      touched: touched[name] || false,
      isDirty: values[name] !== initialValues[name],
    }),
    [values, errors, touched, initialValues]
  );

  /**
   * Check if form is valid
   */
  const isValid = Object.keys(errors).length === 0;

  /**
   * Check if form has been modified
   */
  const hasChanges = useCallback(() => {
    return Object.keys(values).some(key => values[key] !== initialValues[key]);
  }, [values, initialValues]);

  /**
   * Set multiple field values
   */
  const setValues_ = useCallback((newValues) => {
    setValues((prev) => ({
      ...prev,
      ...newValues,
    }));
    setIsDirty(true);
  }, []);

  /**
   * Set multiple errors
   */
  const setErrors_ = useCallback((newErrors) => {
    setErrors((prev) => ({
      ...prev,
      ...newErrors,
    }));
  }, []);

  return {
    // Form state
    values,
    errors,
    touched,
    isSubmitting,
    isDirty,
    isValid,

    // Form methods
    handleChange,
    handleBlur,
    handleSubmit,
    resetForm,

    // Field methods
    setFieldValue,
    setFieldError,
    setFieldTouched,
    setValues: setValues_,
    setErrors: setErrors_,

    // Getters
    getFieldProps,
    getFieldMeta,

    // Utilities
    hasChanges,
  };
};

export default useRiskConfigForm;
