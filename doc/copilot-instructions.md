# Copilot Instructions for stralgo-trade

## Project Overview

This repository contains strategic trading algorithms and related tools. The project focuses on developing, testing, and deploying algorithmic trading strategies.

## General Guidelines

- Write clean, maintainable, and well-documented code
- Follow industry best practices for financial/trading software
- Prioritize code reliability and accuracy - trading algorithms require precision
- Include comprehensive error handling and validation
- Add appropriate logging for debugging and monitoring

## Code Quality Standards

- All code should be thoroughly tested before deployment
- Include unit tests for individual components
- Add integration tests for trading strategies
- Document all functions, classes, and modules with clear docstrings
- Use type hints/annotations where applicable to improve code clarity

## Trading-Specific Requirements

- Always validate input data (prices, volumes, timestamps)
- Handle edge cases (market holidays, trading halts, data gaps)
- Implement proper risk management checks
- Include backtesting capabilities for new strategies
- Document strategy logic, parameters, and expected behavior
- Never commit API keys, credentials, or sensitive trading data

## Security Considerations

- Store credentials and API keys in environment variables
- Use `.gitignore` to exclude sensitive configuration files
- Implement rate limiting for API calls to trading platforms
- Validate and sanitize all external data inputs
- Follow the principle of least privilege for permissions

## Testing Guidelines

- Create tests for all trading strategies before deployment
- Include edge case testing (extreme market conditions, data anomalies)
- Test with historical data where possible
- Mock external API calls in tests to ensure reliability
- Verify calculations independently

## Documentation

- Update README.md with setup instructions and usage examples
- Document strategy parameters and their effects
- Include examples of expected input/output formats
- Maintain a changelog for significant updates
- Document any dependencies and their versions

## Pull Request Process

- Create focused PRs that address specific issues or features
- Include tests that verify the changes
- Update documentation if the changes affect usage
- Reference related issues in PR descriptions
- Ensure all tests pass before requesting review
