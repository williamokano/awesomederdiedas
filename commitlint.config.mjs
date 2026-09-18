// Conventional Commits rules, checked on pull requests by .github/workflows/commitlint.yml
export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    // Allow any capitalization in the description ("fix: Handle X" and "fix: handle X")
    'subject-case': [0],
  },
};
