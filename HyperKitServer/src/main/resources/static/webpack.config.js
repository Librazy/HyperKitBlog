const path = require('path');
const webpack = require('webpack');

module.exports = {
    entry: ["babel-polyfill", "./js/index.js"],
    output: {
        path: __dirname,
        filename: "./js/bundle.js"
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /(node_modules|bower_components)/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: ['env', 'stage-2']
                    }
                }
            }
        ]
    },
    externals: {
        jquery: 'jQuery',
        modernizr: 'Modernizr'
    }
};
