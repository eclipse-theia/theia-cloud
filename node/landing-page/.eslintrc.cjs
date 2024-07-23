/** @type {import('eslint').Linter.Config} */
module.exports = {
  root: true,
  extends: [
    '../configs/base.eslintrc.json',
    '../configs/warnings.eslintrc.json',
    '../configs/errors.eslintrc.json',
    'plugin:react-hooks/recommended'
  ],
  ignorePatterns: ['dist', '.eslintrc.cjs', 'vite.config.ts'],
  parserOptions: {
    tsconfigRootDir: __dirname,
    project: 'tsconfig.json',
  },
  plugins: ['react-refresh'],
  settings: {
    react: {
      version: 'detect'
    }
  },
  rules: {
    'react-refresh/only-export-components': [
      'warn',
      { allowConstantExport: true },
    ],
  },
};
