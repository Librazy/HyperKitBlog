/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, { enumerable: true, get: getter });
/******/ 		}
/******/ 	};
/******/
/******/ 	// define __esModule on exports
/******/ 	__webpack_require__.r = function(exports) {
/******/ 		if(typeof Symbol !== 'undefined' && Symbol.toStringTag) {
/******/ 			Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
/******/ 		}
/******/ 		Object.defineProperty(exports, '__esModule', { value: true });
/******/ 	};
/******/
/******/ 	// create a fake namespace object
/******/ 	// mode & 1: value is a module id, require it
/******/ 	// mode & 2: merge all properties of value into the ns
/******/ 	// mode & 4: return value when already ns object
/******/ 	// mode & 8|1: behave like require
/******/ 	__webpack_require__.t = function(value, mode) {
/******/ 		if(mode & 1) value = __webpack_require__(value);
/******/ 		if(mode & 8) return value;
/******/ 		if((mode & 4) && typeof value === 'object' && value && value.__esModule) return value;
/******/ 		var ns = Object.create(null);
/******/ 		__webpack_require__.r(ns);
/******/ 		Object.defineProperty(ns, 'default', { enumerable: true, value: value });
/******/ 		if(mode & 2 && typeof value != 'string') for(var key in value) __webpack_require__.d(ns, key, function(key) { return value[key]; }.bind(null, key));
/******/ 		return ns;
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = 0);
/******/ })
/************************************************************************/
/******/ ({

/***/ "./login/index.js":
/*!************************!*\
  !*** ./login/index.js ***!
  \************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nfunction _asyncToGenerator(fn) { return function () { var gen = fn.apply(this, arguments); return new Promise(function (resolve, reject) { function step(key, arg) { try { var info = gen[key](arg); var value = info.value; } catch (error) { reject(error); return; } if (info.done) { resolve(value); } else { return Promise.resolve(value).then(function (value) { step(\"next\", value); }, function (err) { step(\"throw\", err); }); } } return step(\"next\"); }); }; }\n\n$(function () {\n    var emailIpt = $(\"#email\");\n    var pwdIpt = $(\"#password\");\n    var signinBtn = $(\"#signin\");\n    var rmCbx = $(\"rememberMe\");\n    signinBtn.click(function () {\n        var _ref = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee(e) {\n            var email, password, rememberMe, re;\n            return regeneratorRuntime.wrap(function _callee$(_context) {\n                while (1) {\n                    switch (_context.prev = _context.next) {\n                        case 0:\n                            signinBtn.attr(\"disabled\", true);\n                            email = emailIpt.val();\n\n                            if (Valid.validEmail(email)) {\n                                _context.next = 5;\n                                break;\n                            }\n\n                            alert(\"邮箱格式错误\");\n                            return _context.abrupt(\"return\");\n\n                        case 5:\n                            password = pwdIpt.val();\n                            rememberMe = rmCbx.prop('checked');\n\n                            if (Valid.validPassword(password)) {\n                                _context.next = 10;\n                                break;\n                            }\n\n                            alert(\"密码长度应在6~100之间\");\n                            return _context.abrupt(\"return\");\n\n                        case 10:\n                            _context.prev = 10;\n                            _context.next = 13;\n                            return Srp.doLogin(email, password, rememberMe);\n\n                        case 13:\n                            re = _context.sent;\n\n                            if (re.status === 200) {\n                                alert(\"登陆成功\");\n                                // TODO\n                            } else {\n                                alert(\"登陆失败 - 用户名或密码错误\");\n                            }\n                            _context.next = 21;\n                            break;\n\n                        case 17:\n                            _context.prev = 17;\n                            _context.t0 = _context[\"catch\"](10);\n\n                            alert(\"很抱歉 - 服务器异常，请稍后重试\");\n                            console.warn(_context.t0);\n\n                        case 21:\n                        case \"end\":\n                            return _context.stop();\n                    }\n                }\n            }, _callee, undefined, [[10, 17]]);\n        }));\n\n        return function (_x) {\n            return _ref.apply(this, arguments);\n        };\n    }());\n});\n\n//# sourceURL=webpack:///./login/index.js?");

/***/ }),

/***/ 0:
/*!*********************!*\
  !*** multi ./login ***!
  \*********************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

eval("module.exports = __webpack_require__(/*! C:\\Users\\Liqueur Librazy\\Git\\HyperKitBlog\\HyperKitServer\\src\\main\\resources\\src\\login */\"./login/index.js\");\n\n\n//# sourceURL=webpack:///multi_./login?");

/***/ })

/******/ });