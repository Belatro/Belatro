module.exports = {
  preset: 'react-native',
  setupFiles: ['./jestSetup.js'],
  transform: {
    '^.+\\.(js|jsx|ts|tsx)$': 'babel-jest',
  },
  transformIgnorePatterns: [
    'node_modules/(?!((jest-)?react-native(-.*)?|@react-native(-community)?|expo(nent)?|@expo(nent)?/.*|react-navigation|@react-navigation/.*|unimodules|sentry-expo|native-base|react-native-vector-icons)/)'
  ],
};
