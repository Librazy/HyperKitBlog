!function(t){var e={};function n(r){if(e[r])return e[r].exports;var o=e[r]={i:r,l:!1,exports:{}};return t[r].call(o.exports,o,o.exports,n),o.l=!0,o.exports}n.m=t,n.c=e,n.d=function(t,e,r){n.o(t,e)||Object.defineProperty(t,e,{enumerable:!0,get:r})},n.r=function(t){"undefined"!=typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(t,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(t,"__esModule",{value:!0})},n.t=function(t,e){if(1&e&&(t=n(t)),8&e)return t;if(4&e&&"object"==typeof t&&t&&t.__esModule)return t;var r=Object.create(null);if(n.r(r),Object.defineProperty(r,"default",{enumerable:!0,value:t}),2&e&&"string"!=typeof t)for(var o in t)n.d(r,o,function(e){return t[e]}.bind(null,o));return r},n.n=function(t){var e=t&&t.__esModule?function(){return t.default}:function(){return t};return n.d(e,"a",e),e},n.o=function(t,e){return Object.prototype.hasOwnProperty.call(t,e)},n.p="",n(n.s=371)}({371:function(t,e,n){t.exports=n(372)},372:function(t,e,n){"use strict";Srp.isSignined()||(window.location.href="/login.html"),$(function(){var t=Srp.uid(),e=$("#avatar"),n=$("#nick"),r=$("#bio"),o=$("#submit");Net.get("/user/"+t+"/").then(function(o){404===o.status&&(alert("用户不存在"),window.location.href="/index.html"),e.val(o.data.avatar),n.val(o.data.nick),r.val(o.data.bio),uInfo(t)(o.data)}),o.click(function(t){return function(){var e=t.apply(this,arguments);return new Promise(function(t,n){return function r(o,i){try{var u=e[o](i),a=u.value}catch(t){return void n(t)}if(!u.done)return Promise.resolve(a).then(function(t){r("next",t)},function(t){r("throw",t)});t(a)}("next")})}}(regeneratorRuntime.mark(function e(){return regeneratorRuntime.wrap(function(e){for(;;)switch(e.prev=e.next){case 0:return o.prop("disabled",!0),e.next=3,Net.put("/user/"+t+"/",{nick:n.val(),avatar:n.val(),bio:r.val()});case 3:200!==e.sent.status&&(alert("修改失败"),o.prop("disabled",!1)),window.location.href="/person.html?id="+t;case 6:case"end":return e.stop()}},e,void 0)})))})}});