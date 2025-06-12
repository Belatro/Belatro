module.exports = {
  jest: function (config) {
    config.transformIgnorePatterns = [
      "/node_modules/(?!axios)/" //transformira axios ili nesto tako?
    ];
    return config;
  },
};