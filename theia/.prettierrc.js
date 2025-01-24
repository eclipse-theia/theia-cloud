module.exports = {
  $schema: 'http://json.schemastore.org/prettierrc',
  singleQuote: true,
  jsxSingleQuote: true,
  arrowParens: 'avoid',
  trailingComma: 'none',
  endOfLine: 'lf',
  printWidth: 120,
  tabWidth: 2,
  overrides: [
    {
      files: ['*.json', '*.yml'],
      options: {
        printWidth: 100,
        tabWidth: 2
      }
    }
  ]
};
