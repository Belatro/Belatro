module.exports = {
  presets: ['module:@react-native/babel-preset'], // or 'module:metro-react-native-babel-preset' for older projects
  plugins: [
    'react-native-reanimated/plugin', // if you use reanimated
    ['@babel/plugin-transform-private-methods', { loose: true }]
  ],
};