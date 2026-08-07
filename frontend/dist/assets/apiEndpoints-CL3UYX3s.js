import{_ as e,d as t,f as n,h as r,t as i,u as a}from"./app-SxtsAmLB.js";var o=e(n(),1);function s(e){var t,n,r=``;if(typeof e==`string`||typeof e==`number`)r+=e;else if(typeof e==`object`)if(Array.isArray(e)){var i=e.length;for(t=0;t<i;t++)e[t]&&(n=s(e[t]))&&(r&&(r+=` `),r+=n)}else for(n in e)e[n]&&(r&&(r+=` `),r+=n);return r}function c(){for(var e,t,n=0,r=``,i=arguments.length;n<i;n++)(e=arguments[n])&&(t=s(e))&&(r&&(r+=` `),r+=t);return r}var l=e=>typeof e==`number`&&!isNaN(e),u=e=>typeof e==`string`,d=e=>typeof e==`function`,f=e=>u(e)||l(e),p=e=>u(e)||d(e)?e:null,m=(e,t)=>e===!1||l(e)&&e>0?e:t,h=e=>(0,o.isValidElement)(e)||u(e)||d(e)||l(e);function g(e,t,n=300){let{scrollHeight:r,style:i}=e;requestAnimationFrame(()=>{i.minHeight=`initial`,i.height=r+`px`,i.transition=`all ${n}ms`,requestAnimationFrame(()=>{i.height=`0`,i.padding=`0`,i.margin=`0`,setTimeout(t,n)})})}function _({enter:e,exit:t,appendPosition:n=!1,collapse:r=!0,collapseDuration:i=300}){return function({children:a,position:s,preventExitTransition:c,done:l,nodeRef:u,isIn:d,playToast:f}){let p=n?`${e}--${s}`:e,m=n?`${t}--${s}`:t,h=(0,o.useRef)(0);return(0,o.useLayoutEffect)(()=>{let e=u.current,t=p.split(` `),n=r=>{r.target===u.current&&(f(),e.removeEventListener(`animationend`,n),e.removeEventListener(`animationcancel`,n),h.current===0&&r.type!==`animationcancel`&&e.classList.remove(...t))};e.classList.add(...t),e.addEventListener(`animationend`,n),e.addEventListener(`animationcancel`,n)},[]),(0,o.useEffect)(()=>{let e=u.current,t=()=>{e.removeEventListener(`animationend`,t),r?g(e,l,i):l()};d||(c?t():(h.current=1,e.className+=` ${m}`,e.addEventListener(`animationend`,t)))},[d]),o.createElement(o.Fragment,null,a)}}function v(e,t){return{content:y(e.content,e.props),containerId:e.props.containerId,id:e.props.toastId,theme:e.props.theme,type:e.props.type,data:e.props.data||{},isLoading:e.props.isLoading,icon:e.props.icon,reason:e.removalReason,status:t}}function y(e,t,n=!1){return(0,o.isValidElement)(e)&&!u(e.type)?(0,o.cloneElement)(e,{closeToast:t.closeToast,toastProps:t,data:t.data,isPaused:n}):d(e)?e({closeToast:t.closeToast,toastProps:t,data:t.data,isPaused:n}):e}function b({closeToast:e,theme:t,ariaLabel:n=`close`}){return o.createElement(`button`,{className:`Toastify__close-button Toastify__close-button--${t}`,type:`button`,onClick:t=>{t.stopPropagation(),e(!0)},"aria-label":n},o.createElement(`svg`,{"aria-hidden":`true`,viewBox:`0 0 14 16`},o.createElement(`path`,{fillRule:`evenodd`,d:`M7.71 8.23l3.75 3.75-1.48 1.48-3.75-3.75-3.75 3.75L1 11.98l3.75-3.75L1 4.48 2.48 3l3.75 3.75L9.98 3l1.48 1.48-3.75 3.75z`})))}function x({delay:e,isRunning:t,closeToast:n,type:r=`default`,hide:i,className:a,controlledProgress:s,progress:l,rtl:u,isIn:f,theme:p}){let m=i||s&&l===0,h={animationDuration:`${e}ms`,animationPlayState:t?`running`:`paused`};s&&(h.transform=`scaleX(${l})`);let g=c(`Toastify__progress-bar`,s?`Toastify__progress-bar--controlled`:`Toastify__progress-bar--animated`,`Toastify__progress-bar-theme--${p}`,`Toastify__progress-bar--${r}`,{"Toastify__progress-bar--rtl":u}),_=d(a)?a({rtl:u,type:r,defaultClassName:g}):c(g,a),v={[s&&l>=1?`onTransitionEnd`:`onAnimationEnd`]:s&&l<1?null:()=>{f&&n()}};return o.createElement(`div`,{className:`Toastify__progress-bar--wrp`,"data-hidden":m},o.createElement(`div`,{className:`Toastify__progress-bar--bg Toastify__progress-bar-theme--${p} Toastify__progress-bar--${r}`}),o.createElement(`div`,{role:`progressbar`,"aria-hidden":m?`true`:`false`,"aria-label":`notification timer`,"aria-valuenow":s?Math.round(l*100):void 0,"aria-valuemin":0,"aria-valuemax":100,className:_,style:h,...v}))}var S=1,C=()=>`${S++}`;function w(e,t,n){let r=1,i=0,a=[],o=[],s=t,c=new Map,u=new Set,d=e=>(u.add(e),()=>u.delete(e)),f=()=>{o=Array.from(c.values()),u.forEach(e=>e())},g=({containerId:t,toastId:n,updateId:r})=>{let i=t?t!==e:e!==1,a=c.has(n)&&r==null;return i||a},_=(e,t)=>{c.forEach(n=>{var r;(t==null||t===n.props.toastId)&&((r=n.toggle)==null||r.call(n,e))})},y=e=>{var t,r;e.isActive&&((r=(t=e.props)?.onClose)==null||r.call(t,e.removalReason),e.isActive=!1,n(v(e,`removed`)))},b=e=>{if(e==null)c.forEach(y);else{let t=c.get(e);t&&y(t)}f()},x=()=>{i-=a.length,a=[]},S=e=>{var t,r;let{toastId:i,updateId:a}=e.props,o=a==null;e.staleId&&c.delete(e.staleId),e.isActive=!0,c.set(i,e),f(),n(v(e,o?`added`:`updated`)),o&&((r=(t=e.props).onOpen)==null||r.call(t))};return{id:e,props:s,observe:d,toggle:_,removeToast:b,toasts:c,clearQueue:x,buildToast:(e,t)=>{if(g(t))return;let{toastId:n,updateId:o,data:u,staleId:d,delay:_}=t,v=o==null;v&&i++;let y={...s,style:s.toastStyle,key:r++,...Object.fromEntries(Object.entries(t).filter(([e,t])=>t!=null)),toastId:n,updateId:o,data:u,isIn:!1,className:p(t.className||s.toastClassName),progressClassName:p(t.progressClassName||s.progressClassName),autoClose:!t.isLoading&&m(t.autoClose,s.autoClose),closeToast(e){let t=c.get(n);t&&(t.removalReason=e,b(n))},deleteToast(){if(c.get(n)!=null){if(c.delete(n),i--,i<0&&(i=0),a.length>0){S(a.shift());return}f()}}};y.closeButton=s.closeButton,t.closeButton===!1||h(t.closeButton)?y.closeButton=t.closeButton:t.closeButton===!0&&(y.closeButton=!h(s.closeButton)||s.closeButton);let x={content:e,props:y,staleId:d};s.limit&&s.limit>0&&i>s.limit&&v?a.push(x):l(_)?setTimeout(()=>{S(x)},_):S(x)},setProps(e){s=e},setToggle:(e,t)=>{let n=c.get(e);n&&(n.toggle=t)},isToastActive:e=>c.get(e)?.isActive,getSnapshot:()=>o}}var T=new Map,E=[],D=new Set,ee=e=>D.forEach(t=>t(e)),te=()=>T.size>0;function O(){E.forEach(e=>N(e.content,e.options)),E=[]}var k=(e,{containerId:t})=>T.get(t||1)?.toasts.get(e);function A(e,t){var n;if(t)return!!((n=T.get(t))!=null&&n.isToastActive(e));let r=!1;return T.forEach(t=>{t.isToastActive(e)&&(r=!0)}),r}function j(e){if(!te()){E=E.filter(t=>e!=null&&t.options.toastId!==e);return}if(e==null||f(e))T.forEach(t=>{t.removeToast(e)});else if(e&&(`containerId`in e||`id`in e)){let t=T.get(e.containerId);t?t.removeToast(e.id):T.forEach(t=>{t.removeToast(e.id)})}}var M=(e={})=>{T.forEach(t=>{t.props.limit&&(!e.containerId||t.id===e.containerId)&&t.clearQueue()})};function N(e,t){h(e)&&(te()||E.push({content:e,options:t}),T.forEach(n=>{n.buildToast(e,t)}))}function ne(e){var t;(t=T.get(e.containerId||1))==null||t.setToggle(e.id,e.fn)}function re(e,t){T.forEach(n=>{(t==null||!(t!=null&&t.containerId)||t?.containerId===n.id)&&n.toggle(e,t?.id)})}function P(e){let t=e.containerId||1;return{subscribe(n){let r=w(t,e,ee);T.set(t,r);let i=r.observe(n);return O(),()=>{i(),T.delete(t)}},setProps(e){var n;(n=T.get(t))==null||n.setProps(e)},getSnapshot(){return T.get(t)?.getSnapshot()}}}function F(e){return D.add(e),()=>{D.delete(e)}}function I(e){return e&&(u(e.toastId)||l(e.toastId))?e.toastId:C()}function L(e,t){return N(e,t),t.toastId}function ie(e,t){return{...t,type:t&&t.type||e,toastId:I(t)}}function ae(e){return(t,n)=>L(t,ie(e,n))}function R(e,t){return L(e,ie(`default`,t))}R.loading=(e,t)=>L(e,ie(`default`,{isLoading:!0,autoClose:!1,closeOnClick:!1,closeButton:!1,draggable:!1,...t}));function oe(e,{pending:t,error:n,success:r},i){let a;t&&(a=u(t)?R.loading(t,i):R.loading(t.render,{...i,...t}));let o={isLoading:null,autoClose:null,closeOnClick:null,closeButton:null,draggable:null},s=(e,t,n)=>{if(t==null){R.dismiss(a);return}let r={type:e,...o,...i,data:n},s=u(t)?{render:t}:t;return a?R.update(a,{...r,...s}):R(s.render,{...r,...s}),n},c=d(e)?e():e;return c.then(e=>s(`success`,r,e)).catch(e=>s(`error`,n,e)),c}R.promise=oe,R.success=ae(`success`),R.info=ae(`info`),R.error=ae(`error`),R.warning=ae(`warning`),R.warn=R.warning,R.dark=(e,t)=>L(e,ie(`default`,{theme:`dark`,...t}));function se(e){j(e)}R.dismiss=se,R.clearWaitingQueue=M,R.isActive=A,R.update=(e,t={})=>{let n=k(e,t);if(n){let{props:r,content:i}=n,a={delay:100,...r,...t,toastId:t.toastId||e,updateId:C()};a.toastId!==e&&(a.staleId=e);let o=a.render||i;delete a.render,L(o,a)}},R.done=e=>{R.update(e,{progress:1})},R.onChange=F,R.play=e=>re(!0,e),R.pause=e=>re(!1,e);function ce(e){let{subscribe:t,getSnapshot:n,setProps:r}=(0,o.useRef)(P(e)).current;r(e);let i=(0,o.useSyncExternalStore)(t,n,n)?.slice();function a(t){if(!i)return[];let n=new Map;return e.newestOnTop&&i.reverse(),i.forEach(e=>{let{position:t}=e.props;n.has(t)||n.set(t,[]),n.get(t).push(e)}),Array.from(n,e=>t(e[0],e[1]))}return{getToastToRender:a,isToastActive:A,count:i?.length}}function le(e){let[t,n]=(0,o.useState)(!1),[r,i]=(0,o.useState)(!1),a=(0,o.useRef)(null),s=(0,o.useRef)({start:0,delta:0,removalDistance:0,canCloseOnClick:!0,canDrag:!1,didMove:!1}).current,{autoClose:c,pauseOnHover:l,closeToast:u,onClick:d,closeOnClick:f}=e;ne({id:e.toastId,containerId:e.containerId,fn:n}),(0,o.useEffect)(()=>{if(e.pauseOnFocusLoss)return p(),()=>{m()}},[e.pauseOnFocusLoss]);function p(){document.hasFocus()||v(),window.addEventListener(`focus`,_),window.addEventListener(`blur`,v)}function m(){window.removeEventListener(`focus`,_),window.removeEventListener(`blur`,v)}function h(t){if(e.draggable===!0||e.draggable===t.pointerType){y();let n=a.current;s.canCloseOnClick=!0,s.canDrag=!0,n.style.transition=`none`,e.draggableDirection===`x`?(s.start=t.clientX,s.removalDistance=n.offsetWidth*(e.draggablePercent/100)):(s.start=t.clientY,s.removalDistance=n.offsetHeight*(e.draggablePercent===80?e.draggablePercent*1.5:e.draggablePercent)/100)}}function g(t){let{top:n,bottom:r,left:i,right:o}=a.current.getBoundingClientRect();t.pointerType===`mouse`&&e.pauseOnHover&&t.clientX>=i&&t.clientX<=o&&t.clientY>=n&&t.clientY<=r?v():_()}function _(){n(!0)}function v(){n(!1)}function y(){s.didMove=!1,document.addEventListener(`pointermove`,x),document.addEventListener(`pointerup`,S)}function b(){document.removeEventListener(`pointermove`,x),document.removeEventListener(`pointerup`,S)}function x(n){let r=a.current;if(s.canDrag&&r){s.didMove=!0,t&&v(),e.draggableDirection===`x`?s.delta=n.clientX-s.start:s.delta=n.clientY-s.start,s.start!==n.clientX&&(s.canCloseOnClick=!1);let i=e.draggableDirection===`x`?`${s.delta}px, var(--y)`:`0, calc(${s.delta}px + var(--y))`;r.style.transform=`translate3d(${i},0)`,r.style.opacity=`${1-Math.abs(s.delta/s.removalDistance)}`}}function S(){b();let t=a.current;if(s.canDrag&&s.didMove&&t){if(s.canDrag=!1,Math.abs(s.delta)>s.removalDistance){i(!0),e.closeToast(!0),e.collapseAll();return}t.style.transition=`transform 0.2s, opacity 0.2s`,t.style.removeProperty(`transform`),t.style.removeProperty(`opacity`)}}let C={onPointerDown:h,onPointerUp:g};return c&&l&&(C.onMouseEnter=v,e.stacked||(C.onMouseLeave=_)),f&&(C.onClick=e=>{d&&d(e),s.canCloseOnClick&&u(!0)}),{playToast:_,pauseToast:v,isRunning:t,preventExitTransition:r,toastRef:a,eventHandlers:C}}var ue=typeof window<`u`?o.useLayoutEffect:o.useEffect,de=({theme:e,type:t,isLoading:n,...r})=>o.createElement(`svg`,{viewBox:`0 0 24 24`,width:`100%`,height:`100%`,fill:e===`colored`?`currentColor`:`var(--toastify-icon-color-${t})`,...r});function fe(e){return o.createElement(de,{...e},o.createElement(`path`,{d:`M23.32 17.191L15.438 2.184C14.728.833 13.416 0 11.996 0c-1.42 0-2.733.833-3.443 2.184L.533 17.448a4.744 4.744 0 000 4.368C1.243 23.167 2.555 24 3.975 24h16.05C22.22 24 24 22.044 24 19.632c0-.904-.251-1.746-.68-2.44zm-9.622 1.46c0 1.033-.724 1.823-1.698 1.823s-1.698-.79-1.698-1.822v-.043c0-1.028.724-1.822 1.698-1.822s1.698.79 1.698 1.822v.043zm.039-12.285l-.84 8.06c-.057.581-.408.943-.897.943-.49 0-.84-.367-.896-.942l-.84-8.065c-.057-.624.25-1.095.779-1.095h1.91c.528.005.84.476.784 1.1z`}))}function pe(e){return o.createElement(de,{...e},o.createElement(`path`,{d:`M12 0a12 12 0 1012 12A12.013 12.013 0 0012 0zm.25 5a1.5 1.5 0 11-1.5 1.5 1.5 1.5 0 011.5-1.5zm2.25 13.5h-4a1 1 0 010-2h.75a.25.25 0 00.25-.25v-4.5a.25.25 0 00-.25-.25h-.75a1 1 0 010-2h1a2 2 0 012 2v4.75a.25.25 0 00.25.25h.75a1 1 0 110 2z`}))}function me(e){return o.createElement(de,{...e},o.createElement(`path`,{d:`M12 0a12 12 0 1012 12A12.014 12.014 0 0012 0zm6.927 8.2l-6.845 9.289a1.011 1.011 0 01-1.43.188l-4.888-3.908a1 1 0 111.25-1.562l4.076 3.261 6.227-8.451a1 1 0 111.61 1.183z`}))}function he(e){return o.createElement(de,{...e},o.createElement(`path`,{d:`M11.983 0a12.206 12.206 0 00-8.51 3.653A11.8 11.8 0 000 12.207 11.779 11.779 0 0011.8 24h.214A12.111 12.111 0 0024 11.791 11.766 11.766 0 0011.983 0zM10.5 16.542a1.476 1.476 0 011.449-1.53h.027a1.527 1.527 0 011.523 1.47 1.475 1.475 0 01-1.449 1.53h-.027a1.529 1.529 0 01-1.523-1.47zM11 12.5v-6a1 1 0 012 0v6a1 1 0 11-2 0z`}))}function ge(){return o.createElement(`div`,{className:`Toastify__spinner`})}var _e={info:pe,warning:fe,success:me,error:he,spinner:ge},ve=e=>e in _e;function ye({theme:e,type:t,isLoading:n,icon:r}){let i=null,a={theme:e,type:t};return r===!1||(d(r)?i=r({...a,isLoading:n}):(0,o.isValidElement)(r)?i=(0,o.cloneElement)(r,a):n?i=_e.spinner():ve(t)&&(i=_e[t](a))),i}var be=e=>{let{isRunning:t,preventExitTransition:n,toastRef:r,eventHandlers:i,playToast:a}=le(e),{closeButton:s,children:l,autoClose:u,onClick:f,type:p,hideProgressBar:m,closeToast:h,transition:g,position:_,className:v,style:S,progressClassName:C,updateId:w,role:T,progress:E,rtl:D,toastId:ee,deleteToast:te,isIn:O,isLoading:k,closeOnClick:A,theme:j,ariaLabel:M}=e,N=c(`Toastify__toast`,`Toastify__toast-theme--${j}`,`Toastify__toast--${p}`,{"Toastify__toast--rtl":D},{"Toastify__toast--close-on-click":A}),ne=d(v)?v({rtl:D,position:_,type:p,defaultClassName:N}):c(N,v),re=ye(e),P=!!E||!u,F={closeToast:h,type:p,theme:j},I=null;return s===!1||(I=d(s)?s(F):(0,o.isValidElement)(s)?(0,o.cloneElement)(s,F):b(F)),o.createElement(g,{isIn:O,done:te,position:_,preventExitTransition:n,nodeRef:r,playToast:a},o.createElement(`div`,{id:ee,tabIndex:0,onClick:f,"data-in":O,className:ne,...i,style:S,ref:r,...O&&{role:T,"aria-label":M}},re!=null&&o.createElement(`div`,{className:c(`Toastify__toast-icon`,{"Toastify--animate-icon Toastify__zoom-enter":!k})},re),y(l,e,!t),I,!e.customProgressBar&&o.createElement(x,{...w&&!P?{key:`p-${w}`}:{},rtl:D,theme:j,delay:u,isRunning:t,isIn:O,closeToast:h,hide:m,type:p,className:C,controlledProgress:P,progress:E||0})))},xe=(e,t=!1)=>({enter:`Toastify--animate Toastify__${e}-enter`,exit:`Toastify--animate Toastify__${e}-exit`,appendPosition:t}),Se=_(xe(`bounce`,!0));_(xe(`slide`,!0)),_(xe(`zoom`)),_(xe(`flip`));var Ce={position:`top-right`,transition:Se,autoClose:5e3,closeButton:!0,pauseOnHover:!0,pauseOnFocusLoss:!0,draggable:`touch`,draggablePercent:80,draggableDirection:`x`,role:`alert`,theme:`light`,"aria-label":`Notifications Alt+T`,hotKeys:e=>e.altKey&&e.code===`KeyT`};function we(e){let t={...Ce,...e},n=e.stacked,[r,i]=(0,o.useState)(!0),a=(0,o.useRef)(null),{getToastToRender:s,isToastActive:l,count:u}=ce(t),{className:f,style:m,rtl:h,containerId:g,hotKeys:_}=t;function v(e){let t=c(`Toastify__toast-container`,`Toastify__toast-container--${e}`,{"Toastify__toast-container--rtl":h});return d(f)?f({position:e,rtl:h,defaultClassName:t}):c(t,p(f))}function y(){n&&(i(!0),R.play())}return ue(()=>{if(n){let e=a.current.querySelectorAll(`[data-in="true"]`),n=t.position?.includes(`top`),i=0,o=0;Array.from(e).reverse().forEach((e,t)=>{let a=e;a.classList.add(`Toastify__toast--stacked`),t>0&&(a.dataset.collapsed=`${r}`),a.dataset.pos||(a.dataset.pos=n?`top`:`bot`);let s=i*(r?.2:1)+(r?0:12*t),c=Math.max(.5,1-(r?o:0));a.style.setProperty(`--y`,`${n?s:s*-1}px`),a.style.setProperty(`--g`,`12`),a.style.setProperty(`--s`,`${c}`),i+=a.offsetHeight,o+=.025})}},[r,u,n]),(0,o.useEffect)(()=>{function e(e){var t;let n=a.current;_(e)&&((t=n?.querySelector(`[tabIndex="0"]`))==null||t.focus(),i(!1),R.pause()),e.key===`Escape`&&(document.activeElement===n||n!=null&&n.contains(document.activeElement))&&(i(!0),R.play())}return document.addEventListener(`keydown`,e),()=>{document.removeEventListener(`keydown`,e)}},[_]),o.createElement(`section`,{ref:a,className:`Toastify`,id:g,onMouseEnter:()=>{n&&(i(!1),R.pause())},onMouseLeave:y,"aria-live":`polite`,"aria-atomic":`false`,"aria-relevant":`additions text`,"aria-label":t[`aria-label`]},s((e,t)=>{let r=t.length?{...m}:{...m,pointerEvents:`none`};return o.createElement(`div`,{tabIndex:-1,className:v(e),"data-stacked":n,style:r,key:`c-${e}`},t.map(({content:e,props:t})=>o.createElement(be,{...t,stacked:n,collapseAll:y,isIn:l(t.toastId,t.containerId),key:`t-${t.key}`},e)))}))}var Te=`:root {
  --toastify-color-light: #fff;
  --toastify-color-dark: #121212;
  --toastify-color-info: #3498db;
  --toastify-color-success: #07bc0c;
  --toastify-color-warning: #f1c40f;
  --toastify-color-error: hsl(6, 78%, 57%);
  --toastify-color-transparent: rgba(255, 255, 255, 0.7);

  --toastify-icon-color-info: var(--toastify-color-info);
  --toastify-icon-color-success: var(--toastify-color-success);
  --toastify-icon-color-warning: var(--toastify-color-warning);
  --toastify-icon-color-error: var(--toastify-color-error);

  --toastify-container-width: fit-content;
  --toastify-toast-width: 320px;
  --toastify-toast-offset: 16px;
  --toastify-toast-top: max(var(--toastify-toast-offset), env(safe-area-inset-top));
  --toastify-toast-right: max(var(--toastify-toast-offset), env(safe-area-inset-right));
  --toastify-toast-left: max(var(--toastify-toast-offset), env(safe-area-inset-left));
  --toastify-toast-bottom: max(var(--toastify-toast-offset), env(safe-area-inset-bottom));
  --toastify-toast-background: #fff;
  --toastify-toast-padding: 14px;
  --toastify-toast-min-height: 64px;
  --toastify-toast-max-height: 800px;
  --toastify-toast-bd-radius: 6px;
  --toastify-toast-shadow: 0px 4px 12px rgba(0, 0, 0, 0.1);
  --toastify-font-family: sans-serif;
  --toastify-z-index: 9999;
  --toastify-text-color-light: #757575;
  --toastify-text-color-dark: #fff;

  /* Used only for colored theme */
  --toastify-text-color-info: #fff;
  --toastify-text-color-success: #fff;
  --toastify-text-color-warning: #fff;
  --toastify-text-color-error: #fff;

  --toastify-spinner-color: #616161;
  --toastify-spinner-color-empty-area: #e0e0e0;
  --toastify-color-progress-light: linear-gradient(to right, #4cd964, #5ac8fa, #007aff, #34aadc, #5856d6, #ff2d55);
  --toastify-color-progress-dark: #bb86fc;
  --toastify-color-progress-info: var(--toastify-color-info);
  --toastify-color-progress-success: var(--toastify-color-success);
  --toastify-color-progress-warning: var(--toastify-color-warning);
  --toastify-color-progress-error: var(--toastify-color-error);
  /* used to control the opacity of the progress trail */
  --toastify-color-progress-bgo: 0.2;
}

.Toastify__toast-container {
  z-index: var(--toastify-z-index);
  -webkit-transform: translate3d(0, 0, var(--toastify-z-index));
  position: fixed;
  width: var(--toastify-container-width);
  box-sizing: border-box;
  color: #fff;
  display: flex;
  flex-direction: column;
}

.Toastify__toast-container--top-left {
  top: var(--toastify-toast-top);
  left: var(--toastify-toast-left);
}
.Toastify__toast-container--top-center {
  top: var(--toastify-toast-top);
  left: 50%;
  transform: translateX(-50%);
  align-items: center;
}
.Toastify__toast-container--top-right {
  top: var(--toastify-toast-top);
  right: var(--toastify-toast-right);
  align-items: end;
}
.Toastify__toast-container--bottom-left {
  bottom: var(--toastify-toast-bottom);
  left: var(--toastify-toast-left);
}
.Toastify__toast-container--bottom-center {
  bottom: var(--toastify-toast-bottom);
  left: 50%;
  transform: translateX(-50%);
  align-items: center;
}
.Toastify__toast-container--bottom-right {
  bottom: var(--toastify-toast-bottom);
  right: var(--toastify-toast-right);
  align-items: end;
}

.Toastify__toast {
  --y: 0px;
  position: relative;
  touch-action: none;
  width: var(--toastify-toast-width);
  min-height: var(--toastify-toast-min-height);
  box-sizing: border-box;
  margin-bottom: 1rem;
  padding: var(--toastify-toast-padding);
  border-radius: var(--toastify-toast-bd-radius);
  box-shadow: var(--toastify-toast-shadow);
  max-height: var(--toastify-toast-max-height);
  font-family: var(--toastify-font-family);
  /* webkit only issue #791 */
  z-index: 0;
  /* inner swag */
  display: flex;
  flex: 1 auto;
  align-items: center;
  word-break: break-word;
}

@media only screen and (max-width: 480px) {
  .Toastify__toast-container {
    width: 100vw;
    left: env(safe-area-inset-left);
    margin: 0;
  }
  .Toastify__toast-container--top-left,
  .Toastify__toast-container--top-center,
  .Toastify__toast-container--top-right {
    top: env(safe-area-inset-top);
    transform: translateX(0);
  }
  .Toastify__toast-container--bottom-left,
  .Toastify__toast-container--bottom-center,
  .Toastify__toast-container--bottom-right {
    bottom: env(safe-area-inset-bottom);
    transform: translateX(0);
  }
  .Toastify__toast-container--rtl {
    right: env(safe-area-inset-right);
    left: initial;
  }
  .Toastify__toast {
    --toastify-toast-width: 100%;
    margin-bottom: 0;
    border-radius: 0;
  }
}

.Toastify__toast-container[data-stacked='true'] {
  width: var(--toastify-toast-width);
}

@media only screen and (max-width: 480px) {
  .Toastify__toast-container[data-stacked='true'] {
    width: 100vw;
  }
}

.Toastify__toast--stacked {
  position: absolute;
  width: 100%;
  transform: translate3d(0, var(--y), 0) scale(var(--s));
  transition: transform 0.3s;
}

.Toastify__toast--stacked[data-collapsed] .Toastify__toast-body,
.Toastify__toast--stacked[data-collapsed] .Toastify__close-button {
  transition: opacity 0.1s;
}

.Toastify__toast--stacked[data-collapsed='false'] {
  overflow: visible;
}

.Toastify__toast--stacked[data-collapsed='true']:not(:last-child) > * {
  opacity: 0;
}

.Toastify__toast--stacked:after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: calc(var(--g) * 1px);
  bottom: 100%;
}

.Toastify__toast--stacked[data-pos='top'] {
  top: 0;
}

.Toastify__toast--stacked[data-pos='bot'] {
  bottom: 0;
}

.Toastify__toast--stacked[data-pos='bot'].Toastify__toast--stacked:before {
  transform-origin: top;
}

.Toastify__toast--stacked[data-pos='top'].Toastify__toast--stacked:before {
  transform-origin: bottom;
}

.Toastify__toast--stacked:before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 100%;
  transform: scaleY(3);
  z-index: -1;
}

.Toastify__toast--rtl {
  direction: rtl;
}

.Toastify__toast--close-on-click {
  cursor: pointer;
}

.Toastify__toast-icon {
  margin-inline-end: 10px;
  width: 22px;
  flex-shrink: 0;
  display: flex;
}

.Toastify--animate {
  animation-fill-mode: both;
  animation-duration: 0.5s;
}

.Toastify--animate-icon {
  animation-fill-mode: both;
  animation-duration: 0.3s;
}

.Toastify__toast-theme--dark {
  background: var(--toastify-color-dark);
  color: var(--toastify-text-color-dark);
}

.Toastify__toast-theme--light {
  background: var(--toastify-color-light);
  color: var(--toastify-text-color-light);
}

.Toastify__toast-theme--colored.Toastify__toast--default {
  background: var(--toastify-color-light);
  color: var(--toastify-text-color-light);
}

.Toastify__toast-theme--colored.Toastify__toast--info {
  color: var(--toastify-text-color-info);
  background: var(--toastify-color-info);
}

.Toastify__toast-theme--colored.Toastify__toast--success {
  color: var(--toastify-text-color-success);
  background: var(--toastify-color-success);
}

.Toastify__toast-theme--colored.Toastify__toast--warning {
  color: var(--toastify-text-color-warning);
  background: var(--toastify-color-warning);
}

.Toastify__toast-theme--colored.Toastify__toast--error {
  color: var(--toastify-text-color-error);
  background: var(--toastify-color-error);
}

.Toastify__progress-bar-theme--light {
  background: var(--toastify-color-progress-light);
}

.Toastify__progress-bar-theme--dark {
  background: var(--toastify-color-progress-dark);
}

.Toastify__progress-bar--info {
  background: var(--toastify-color-progress-info);
}

.Toastify__progress-bar--success {
  background: var(--toastify-color-progress-success);
}

.Toastify__progress-bar--warning {
  background: var(--toastify-color-progress-warning);
}

.Toastify__progress-bar--error {
  background: var(--toastify-color-progress-error);
}

.Toastify__progress-bar-theme--colored.Toastify__progress-bar--info,
.Toastify__progress-bar-theme--colored.Toastify__progress-bar--success,
.Toastify__progress-bar-theme--colored.Toastify__progress-bar--warning,
.Toastify__progress-bar-theme--colored.Toastify__progress-bar--error {
  background: var(--toastify-color-transparent);
}

.Toastify__close-button {
  color: #fff;
  position: absolute;
  top: 6px;
  right: 6px;
  background: transparent;
  outline: none;
  border: none;
  padding: 0;
  cursor: pointer;
  opacity: 0.7;
  transition: 0.3s ease;
  z-index: 1;
}

.Toastify__toast--rtl .Toastify__close-button {
  left: 6px;
  right: unset;
}

.Toastify__close-button--light {
  color: #000;
  opacity: 0.3;
}

.Toastify__close-button > svg {
  fill: currentColor;
  height: 16px;
  width: 14px;
}

.Toastify__close-button:hover,
.Toastify__close-button:focus {
  opacity: 1;
}

@keyframes Toastify__trackProgress {
  0% {
    transform: scaleX(1);
  }
  100% {
    transform: scaleX(0);
  }
}

.Toastify__progress-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  opacity: 0.7;
  transform-origin: left;
}

.Toastify__progress-bar--animated {
  animation: Toastify__trackProgress linear 1 forwards;
}

.Toastify__progress-bar--controlled {
  transition: transform 0.2s;
}

.Toastify__progress-bar--rtl {
  right: 0;
  left: initial;
  transform-origin: right;
  border-bottom-left-radius: initial;
}

.Toastify__progress-bar--wrp {
  position: absolute;
  overflow: hidden;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 5px;
  border-bottom-left-radius: var(--toastify-toast-bd-radius);
  border-bottom-right-radius: var(--toastify-toast-bd-radius);
}

.Toastify__progress-bar--wrp[data-hidden='true'] {
  opacity: 0;
}

.Toastify__progress-bar--bg {
  opacity: var(--toastify-color-progress-bgo);
  width: 100%;
  height: 100%;
}

.Toastify__spinner {
  width: 20px;
  height: 20px;
  box-sizing: border-box;
  border: 2px solid;
  border-radius: 100%;
  border-color: var(--toastify-spinner-color-empty-area);
  border-right-color: var(--toastify-spinner-color);
  animation: Toastify__spin 0.65s linear infinite;
}

@keyframes Toastify__bounceInRight {
  from,
  60%,
  75%,
  90%,
  to {
    animation-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1);
  }
  from {
    opacity: 0;
    transform: translate3d(3000px, 0, 0);
  }
  60% {
    opacity: 1;
    transform: translate3d(-25px, 0, 0);
  }
  75% {
    transform: translate3d(10px, 0, 0);
  }
  90% {
    transform: translate3d(-5px, 0, 0);
  }
  to {
    transform: none;
  }
}

@keyframes Toastify__bounceOutRight {
  20% {
    opacity: 1;
    transform: translate3d(-20px, var(--y), 0);
  }
  to {
    opacity: 0;
    transform: translate3d(2000px, var(--y), 0);
  }
}

@keyframes Toastify__bounceInLeft {
  from,
  60%,
  75%,
  90%,
  to {
    animation-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1);
  }
  0% {
    opacity: 0;
    transform: translate3d(-3000px, 0, 0);
  }
  60% {
    opacity: 1;
    transform: translate3d(25px, 0, 0);
  }
  75% {
    transform: translate3d(-10px, 0, 0);
  }
  90% {
    transform: translate3d(5px, 0, 0);
  }
  to {
    transform: none;
  }
}

@keyframes Toastify__bounceOutLeft {
  20% {
    opacity: 1;
    transform: translate3d(20px, var(--y), 0);
  }
  to {
    opacity: 0;
    transform: translate3d(-2000px, var(--y), 0);
  }
}

@keyframes Toastify__bounceInUp {
  from,
  60%,
  75%,
  90%,
  to {
    animation-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1);
  }
  from {
    opacity: 0;
    transform: translate3d(0, 3000px, 0);
  }
  60% {
    opacity: 1;
    transform: translate3d(0, -20px, 0);
  }
  75% {
    transform: translate3d(0, 10px, 0);
  }
  90% {
    transform: translate3d(0, -5px, 0);
  }
  to {
    transform: translate3d(0, 0, 0);
  }
}

@keyframes Toastify__bounceOutUp {
  20% {
    transform: translate3d(0, calc(var(--y) - 10px), 0);
  }
  40%,
  45% {
    opacity: 1;
    transform: translate3d(0, calc(var(--y) + 20px), 0);
  }
  to {
    opacity: 0;
    transform: translate3d(0, -2000px, 0);
  }
}

@keyframes Toastify__bounceInDown {
  from,
  60%,
  75%,
  90%,
  to {
    animation-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1);
  }
  0% {
    opacity: 0;
    transform: translate3d(0, -3000px, 0);
  }
  60% {
    opacity: 1;
    transform: translate3d(0, 25px, 0);
  }
  75% {
    transform: translate3d(0, -10px, 0);
  }
  90% {
    transform: translate3d(0, 5px, 0);
  }
  to {
    transform: none;
  }
}

@keyframes Toastify__bounceOutDown {
  20% {
    transform: translate3d(0, calc(var(--y) - 10px), 0);
  }
  40%,
  45% {
    opacity: 1;
    transform: translate3d(0, calc(var(--y) + 20px), 0);
  }
  to {
    opacity: 0;
    transform: translate3d(0, 2000px, 0);
  }
}

.Toastify__bounce-enter--top-left,
.Toastify__bounce-enter--bottom-left {
  animation-name: Toastify__bounceInLeft;
}

.Toastify__bounce-enter--top-right,
.Toastify__bounce-enter--bottom-right {
  animation-name: Toastify__bounceInRight;
}

.Toastify__bounce-enter--top-center {
  animation-name: Toastify__bounceInDown;
}

.Toastify__bounce-enter--bottom-center {
  animation-name: Toastify__bounceInUp;
}

.Toastify__bounce-exit--top-left,
.Toastify__bounce-exit--bottom-left {
  animation-name: Toastify__bounceOutLeft;
}

.Toastify__bounce-exit--top-right,
.Toastify__bounce-exit--bottom-right {
  animation-name: Toastify__bounceOutRight;
}

.Toastify__bounce-exit--top-center {
  animation-name: Toastify__bounceOutUp;
}

.Toastify__bounce-exit--bottom-center {
  animation-name: Toastify__bounceOutDown;
}

@keyframes Toastify__zoomIn {
  from {
    opacity: 0;
    transform: scale3d(0.3, 0.3, 0.3);
  }
  50% {
    opacity: 1;
  }
}

@keyframes Toastify__zoomOut {
  from {
    opacity: 1;
  }
  50% {
    opacity: 0;
    transform: translate3d(0, var(--y), 0) scale3d(0.3, 0.3, 0.3);
  }
  to {
    opacity: 0;
  }
}

.Toastify__zoom-enter {
  animation-name: Toastify__zoomIn;
}

.Toastify__zoom-exit {
  animation-name: Toastify__zoomOut;
}

@keyframes Toastify__flipIn {
  from {
    transform: perspective(400px) rotate3d(1, 0, 0, 90deg);
    animation-timing-function: ease-in;
    opacity: 0;
  }
  40% {
    transform: perspective(400px) rotate3d(1, 0, 0, -20deg);
    animation-timing-function: ease-in;
  }
  60% {
    transform: perspective(400px) rotate3d(1, 0, 0, 10deg);
    opacity: 1;
  }
  80% {
    transform: perspective(400px) rotate3d(1, 0, 0, -5deg);
  }
  to {
    transform: perspective(400px);
  }
}

@keyframes Toastify__flipOut {
  from {
    transform: translate3d(0, var(--y), 0) perspective(400px);
  }
  30% {
    transform: translate3d(0, var(--y), 0) perspective(400px) rotate3d(1, 0, 0, -20deg);
    opacity: 1;
  }
  to {
    transform: translate3d(0, var(--y), 0) perspective(400px) rotate3d(1, 0, 0, 90deg);
    opacity: 0;
  }
}

.Toastify__flip-enter {
  animation-name: Toastify__flipIn;
}

.Toastify__flip-exit {
  animation-name: Toastify__flipOut;
}

@keyframes Toastify__slideInRight {
  from {
    transform: translate3d(110%, 0, 0);
    visibility: visible;
  }
  to {
    transform: translate3d(0, var(--y), 0);
  }
}

@keyframes Toastify__slideInLeft {
  from {
    transform: translate3d(-110%, 0, 0);
    visibility: visible;
  }
  to {
    transform: translate3d(0, var(--y), 0);
  }
}

@keyframes Toastify__slideInUp {
  from {
    transform: translate3d(0, 110%, 0);
    visibility: visible;
  }
  to {
    transform: translate3d(0, var(--y), 0);
  }
}

@keyframes Toastify__slideInDown {
  from {
    transform: translate3d(0, -110%, 0);
    visibility: visible;
  }
  to {
    transform: translate3d(0, var(--y), 0);
  }
}

@keyframes Toastify__slideOutRight {
  from {
    transform: translate3d(0, var(--y), 0);
  }
  to {
    visibility: hidden;
    transform: translate3d(110%, var(--y), 0);
  }
}

@keyframes Toastify__slideOutLeft {
  from {
    transform: translate3d(0, var(--y), 0);
  }
  to {
    visibility: hidden;
    transform: translate3d(-110%, var(--y), 0);
  }
}

@keyframes Toastify__slideOutDown {
  from {
    transform: translate3d(0, var(--y), 0);
  }
  to {
    visibility: hidden;
    transform: translate3d(0, 500px, 0);
  }
}

@keyframes Toastify__slideOutUp {
  from {
    transform: translate3d(0, var(--y), 0);
  }
  to {
    visibility: hidden;
    transform: translate3d(0, -500px, 0);
  }
}

.Toastify__slide-enter--top-left,
.Toastify__slide-enter--bottom-left {
  animation-name: Toastify__slideInLeft;
}

.Toastify__slide-enter--top-right,
.Toastify__slide-enter--bottom-right {
  animation-name: Toastify__slideInRight;
}

.Toastify__slide-enter--top-center {
  animation-name: Toastify__slideInDown;
}

.Toastify__slide-enter--bottom-center {
  animation-name: Toastify__slideInUp;
}

.Toastify__slide-exit--top-left,
.Toastify__slide-exit--bottom-left {
  animation-name: Toastify__slideOutLeft;
  animation-timing-function: ease-in;
  animation-duration: 0.3s;
}

.Toastify__slide-exit--top-right,
.Toastify__slide-exit--bottom-right {
  animation-name: Toastify__slideOutRight;
  animation-timing-function: ease-in;
  animation-duration: 0.3s;
}

.Toastify__slide-exit--top-center {
  animation-name: Toastify__slideOutUp;
  animation-timing-function: ease-in;
  animation-duration: 0.3s;
}

.Toastify__slide-exit--bottom-center {
  animation-name: Toastify__slideOutDown;
  animation-timing-function: ease-in;
  animation-duration: 0.3s;
}

@keyframes Toastify__spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
`,Ee=new Map,De=(e,t)=>{ue(()=>{if(!e||typeof document>`u`)return;let n=document,r=Ee.get(n);if(r){t&&r.setAttribute(`nonce`,t);return}let i=n.createElement(`style`);i.textContent=e,t&&i.setAttribute(`nonce`,t),n.head.appendChild(i),Ee.set(n,i)},[t])};function Oe(e){return De(Te,e.nonce),o.createElement(we,{...e})}var ke=e=>{let t,n=new Set,r=(e,r)=>{let i=typeof e==`function`?e(t):e;if(!Object.is(i,t)){let e=t;t=r??(typeof i!=`object`||!i)?i:Object.assign({},t,i),n.forEach(n=>n(t,e))}},i=()=>t,a={setState:r,getState:i,getInitialState:()=>o,subscribe:e=>(n.add(e),()=>n.delete(e))},o=t=e(r,i,a);return a},Ae=(e=>e?ke(e):ke),je=e=>e;function Me(e,t=je){let n=o.useSyncExternalStore(e.subscribe,o.useCallback(()=>t(e.getState()),[e,t]),o.useCallback(()=>t(e.getInitialState()),[e,t]));return o.useDebugValue(n),n}var Ne=e=>{let t=Ae(e),n=e=>Me(t,e);return Object.assign(n,t),n},Pe=(e=>e?Ne(e):Ne);function Fe(e,t){let n;try{n=e()}catch{return}return{getItem:e=>{let r=e=>e===null?null:JSON.parse(e,t?.reviver),i=n.getItem(e)??null;return i instanceof Promise?i.then(r):r(i)},setItem:(e,r)=>n.setItem(e,JSON.stringify(r,t?.replacer)),removeItem:e=>n.removeItem(e)}}var Ie=e=>t=>{try{let n=e(t);return n instanceof Promise?n:{then(e){return Ie(e)(n)},catch(e){return this}}}catch(e){return{then(e){return this},catch(t){return Ie(t)(e)}}}},Le=(e,t)=>(n,r,i)=>{let a={storage:Fe(()=>window.localStorage),partialize:e=>e,version:0,merge:(e,t)=>({...t,...e}),...t},o=!1,s=0,c=new Set,l=new Set,u=a.storage;if(!u)return e((...e)=>{console.warn(`[zustand persist middleware] Unable to update item '${a.name}', the given storage is currently unavailable.`),n(...e)},r,i);let d=()=>{let e=a.partialize({...r()});return u.setItem(a.name,{state:e,version:a.version})},f=i.setState;i.setState=(e,t)=>(f(e,t),d());let p=e((...e)=>(n(...e),d()),r,i);i.getInitialState=()=>p;let m,h=()=>{if(!u)return;let e=++s;o=!1,c.forEach(e=>e(r()??p));let t=a.onRehydrateStorage?.call(a,r()??p)||void 0;return Ie(u.getItem.bind(u))(a.name).then(e=>{if(e)if(typeof e.version==`number`&&e.version!==a.version){if(a.migrate){let t=a.migrate(e.state,e.version);return t instanceof Promise?t.then(e=>[!0,e]):[!0,t]}console.error(`State loaded from storage couldn't be migrated since no migrate function was provided`)}else return[!1,e.state];return[!1,void 0]}).then(t=>{if(e!==s)return;let[i,o]=t;if(m=a.merge(o,r()??p),n(m,!0),i)return d()}).then(()=>{e===s&&(t?.(r(),void 0),m=r(),o=!0,l.forEach(e=>e(m)))}).catch(n=>{e===s&&t?.(void 0,n)})};return i.persist={setOptions:e=>{a={...a,...e},e.storage&&(u=e.storage)},clearStorage:()=>{u?.removeItem(a.name)},getOptions:()=>a,rehydrate:()=>h(),hasHydrated:()=>o,onHydrate:e=>(c.add(e),()=>{c.delete(e)}),onFinishHydration:e=>(l.add(e),()=>{l.delete(e)})},a.skipHydration||h(),m||p},Re={auth:`cloudnest-auth`,theme:`cloudnest-theme`,ui:`cloudnest-ui`,files:`cloudnest-files`,folders:`cloudnest-folders`,recentSearches:`cloudnest-recent-searches`,deviceId:`cloudnest-device-id`};function ze(e,t){return function(){return e.apply(t,arguments)}}var{toString:Be}=Object.prototype,{getPrototypeOf:Ve}=Object,{iterator:He,toStringTag:Ue}=Symbol,We=(({hasOwnProperty:e})=>(t,n)=>e.call(t,n))(Object.prototype),Ge=(e,t)=>{let n=e,r=[];for(;n!=null&&n!==Object.prototype;){if(r.indexOf(n)!==-1)return!1;if(r.push(n),We(n,t))return!0;n=Ve(n)}return!1},Ke=(e,t)=>e!=null&&Ge(e,t)?e[t]:void 0,qe=(e=>t=>{let n=Be.call(t);return e[n]||(e[n]=n.slice(8,-1).toLowerCase())})(Object.create(null)),z=e=>(e=e.toLowerCase(),t=>qe(t)===e),Je=e=>t=>typeof t===e,{isArray:B}=Array,V=Je(`undefined`);function H(e){return e!==null&&!V(e)&&e.constructor!==null&&!V(e.constructor)&&U(e.constructor.isBuffer)&&e.constructor.isBuffer(e)}var Ye=z(`ArrayBuffer`);function Xe(e){let t;return t=typeof ArrayBuffer<`u`&&ArrayBuffer.isView?ArrayBuffer.isView(e):e&&e.buffer&&Ye(e.buffer),t}var Ze=Je(`string`),U=Je(`function`),Qe=Je(`number`),W=e=>typeof e==`object`&&!!e,$e=e=>e===!0||e===!1,et=e=>{if(!W(e))return!1;let t=Ve(e);return(t===null||t===Object.prototype||Ve(t)===null)&&!Ge(e,Ue)&&!Ge(e,He)},tt=e=>{if(!W(e)||H(e))return!1;try{return Object.keys(e).length===0&&Object.getPrototypeOf(e)===Object.prototype}catch{return!1}},nt=z(`Date`),rt=z(`File`),it=e=>!!(e&&e.uri!==void 0),at=e=>e&&e.getParts!==void 0,ot=z(`Blob`),st=z(`FileList`),ct=z(`Set`),lt=e=>W(e)&&U(e.pipe);function ut(){return typeof globalThis<`u`?globalThis:typeof self<`u`?self:typeof window<`u`?window:typeof global<`u`?global:{}}var dt=ut(),ft=dt.FormData===void 0?void 0:dt.FormData,pt=e=>{if(!e)return!1;if(ft&&e instanceof ft)return!0;let t=Ve(e);if(!t||t===Object.prototype||!U(e.append))return!1;let n=qe(e);return n===`formdata`||n===`object`&&U(e.toString)&&e.toString()===`[object FormData]`},mt=z(`URLSearchParams`),[ht,gt,_t,vt]=[`ReadableStream`,`Request`,`Response`,`Headers`].map(z),yt=e=>e.trim?e.trim():e.replace(/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g,``);function bt(e,t,{allOwnKeys:n=!1}={}){if(e==null)return;let r,i;if(typeof e!=`object`&&(e=[e]),B(e))for(r=0,i=e.length;r<i;r++)t.call(null,e[r],r,e);else{if(H(e))return;let i=n?Object.getOwnPropertyNames(e):Object.keys(e),a=i.length,o;for(r=0;r<a;r++)o=i[r],t.call(null,e[o],o,e)}}function xt(e,t){if(H(e))return null;t=t.toLowerCase();let n=Object.keys(e),r=n.length,i;for(;r-->0;)if(i=n[r],t===i.toLowerCase())return i;return null}var G=typeof globalThis<`u`?globalThis:typeof self<`u`?self:typeof window<`u`?window:global,St=e=>!V(e)&&e!==G;function Ct(...e){let{caseless:t,skipUndefined:n}=St(this)&&this||{},r={},i=(e,i)=>{if(i===`__proto__`||i===`constructor`||i===`prototype`)return;let a=t&&typeof i==`string`&&xt(r,i)||i,o=We(r,a)?r[a]:void 0;et(o)&&et(e)?r[a]=Ct(o,e):et(e)?r[a]=Ct({},e):B(e)?r[a]=e.slice():(!n||!V(e))&&(r[a]=e)};for(let t=0,n=e.length;t<n;t++){let n=e[t];if(!n||H(n)||(bt(n,i),typeof n!=`object`||B(n)))continue;let r=Object.getOwnPropertySymbols(n);for(let e=0;e<r.length;e++){let t=r[e];Ft.call(n,t)&&i(n[t],t)}}return r}var wt=(e,t,n,{allOwnKeys:r}={})=>(bt(t,(t,r)=>{n&&U(t)?Object.defineProperty(e,r,{__proto__:null,value:ze(t,n),writable:!0,enumerable:!0,configurable:!0}):Object.defineProperty(e,r,{__proto__:null,value:t,writable:!0,enumerable:!0,configurable:!0})},{allOwnKeys:r}),e),Tt=e=>(e.charCodeAt(0)===65279&&(e=e.slice(1)),e),Et=(e,t,n,r)=>{e.prototype=Object.create(t.prototype,r),Object.defineProperty(e.prototype,"constructor",{__proto__:null,value:e,writable:!0,enumerable:!1,configurable:!0}),Object.defineProperty(e,"super",{__proto__:null,value:t.prototype}),n&&Object.assign(e.prototype,n)},Dt=(e,t,n,r)=>{let i,a,o,s={};if(t||={},e==null)return t;do{for(i=Object.getOwnPropertyNames(e),a=i.length;a-->0;)o=i[a],(!r||r(o,e,t))&&!s[o]&&(t[o]=e[o],s[o]=!0);e=n!==!1&&Ve(e)}while(e&&(!n||n(e,t))&&e!==Object.prototype);return t},Ot=(e,t,n)=>{e=String(e),(n===void 0||n>e.length)&&(n=e.length),n-=t.length;let r=e.indexOf(t,n);return r!==-1&&r===n},kt=e=>{if(!e)return null;if(B(e))return e;let t=e.length;if(!Qe(t))return null;let n=Array(t);for(;t-->0;)n[t]=e[t];return n},At=(e=>t=>e&&t instanceof e)(typeof Uint8Array<`u`&&Ve(Uint8Array)),jt=(e,t)=>{let n=(e&&e[He]).call(e),r;for(;(r=n.next())&&!r.done;){let n=r.value;t.call(e,n[0],n[1])}},Mt=(e,t)=>{let n,r=[];for(;(n=e.exec(t))!==null;)r.push(n);return r},Nt=z(`HTMLFormElement`),Pt=e=>e.toLowerCase().replace(/[-_\s]([a-z\d])(\w*)/g,function(e,t,n){return t.toUpperCase()+n}),{propertyIsEnumerable:Ft}=Object.prototype,It=z(`RegExp`),Lt=(e,t)=>{let n=Object.getOwnPropertyDescriptors(e),r={};bt(n,(n,i)=>{let a;(a=t(n,i,e))!==!1&&(r[i]=a||n)}),Object.defineProperties(e,r)},Rt=e=>{Lt(e,(t,n)=>{if(U(e)&&[`arguments`,`caller`,`callee`].includes(n))return!1;let r=e[n];if(U(r)){if(t.enumerable=!1,`writable`in t){t.writable=!1;return}t.set||=()=>{throw Error(`Can not rewrite read-only method '`+n+`'`)}}})},zt=(e,t)=>{let n={},r=e=>{e.forEach(e=>{n[e]=!0})};return B(e)?r(e):r(String(e).split(t)),n},Bt=()=>{},Vt=(e,t)=>e!=null&&Number.isFinite(e=+e)?e:t;function Ht(e){return!!(e&&U(e.append)&&e[Ue]===`FormData`&&e[He])}var Ut=e=>{let t=new WeakSet,n=e=>{if(W(e)){if(t.has(e))return;if(H(e))return e;if(!(`toJSON`in e)){t.add(e);let r;if(ct(e)){r=[];for(let t of e){let e=n(t);!V(e)&&r.push(e)}}else r=B(e)?[]:{},bt(e,(e,t)=>{let i=n(e);!V(i)&&(r[t]=i)});return t.delete(e),r}}return e};return n(e)},Wt=z(`AsyncFunction`),Gt=e=>e&&(W(e)||U(e))&&U(e.then)&&U(e.catch),Kt=((e,t)=>e?setImmediate:t?((e,t)=>(G.addEventListener(`message`,({source:n,data:r})=>{n===G&&r===e&&t.length&&t.shift()()},!1),n=>{t.push(n),G.postMessage(e,`*`)}))(`axios@${Math.random()}`,[]):e=>setTimeout(e))(typeof setImmediate==`function`,U(G.postMessage)),qt=typeof queueMicrotask<`u`?queueMicrotask.bind(G):typeof process<`u`&&process.nextTick||Kt,Jt=e=>e!=null&&U(e[He]),K={isArray:B,isArrayBuffer:Ye,isBuffer:H,isFormData:pt,isArrayBufferView:Xe,isString:Ze,isNumber:Qe,isBoolean:$e,isObject:W,isPlainObject:et,isEmptyObject:tt,isReadableStream:ht,isRequest:gt,isResponse:_t,isHeaders:vt,isUndefined:V,isDate:nt,isFile:rt,isReactNativeBlob:it,isReactNative:at,isBlob:ot,isRegExp:It,isFunction:U,isStream:lt,isURLSearchParams:mt,isTypedArray:At,isFileList:st,forEach:bt,merge:Ct,extend:wt,trim:yt,stripBOM:Tt,inherits:Et,toFlatObject:Dt,kindOf:qe,kindOfTest:z,endsWith:Ot,toArray:kt,forEachEntry:jt,matchAll:Mt,isHTMLForm:Nt,hasOwnProperty:We,hasOwnProp:We,hasOwnInPrototypeChain:Ge,getSafeProp:Ke,reduceDescriptors:Lt,freezeMethods:Rt,toObjectSet:zt,toCamelCase:Pt,noop:Bt,toFiniteNumber:Vt,findKey:xt,global:G,isContextDefined:St,isSpecCompliantForm:Ht,toJSONObject:Ut,isAsyncFn:Wt,isThenable:Gt,setImmediate:Kt,asap:qt,isIterable:Jt,isSafeIterable:e=>e!=null&&Ge(e,He)&&Jt(e)},Yt=K.toObjectSet([`age`,`authorization`,`content-length`,`content-type`,`etag`,`expires`,`from`,`host`,`if-modified-since`,`if-unmodified-since`,`last-modified`,`location`,`max-forwards`,`proxy-authorization`,`referer`,`retry-after`,`user-agent`]),Xt=e=>{let t={},n,r,i;return e&&e.split(`
`).forEach(function(e){i=e.indexOf(`:`),n=e.substring(0,i).trim().toLowerCase(),r=e.substring(i+1).trim();let a=K.hasOwnProp(t,n);!n||a&&K.hasOwnProp(Yt,n)||(n===`set-cookie`?a?t[n].push(r):t[n]=[r]:t[n]=a?t[n]+`, `+r:r)}),t};function Zt(e){let t=0,n=e.length;for(;t<n;){let n=e.charCodeAt(t);if(n!==9&&n!==32)break;t+=1}for(;n>t;){let t=e.charCodeAt(n-1);if(t!==9&&t!==32)break;--n}return t===0&&n===e.length?e:e.slice(t,n)}var Qt=RegExp(`[\\u0000-\\u0008\\u000a-\\u001f\\u007f]+`,`g`),$t=RegExp(`[^\\u0009\\u0020-\\u007e\\u0080-\\u00ff]+`,`g`);function en(e,t){return K.isArray(e)?e.map(e=>en(e,t)):Zt(String(e).replace(t,``))}var tn=e=>en(e,Qt),nn=e=>en(e,$t);function rn(e){let t=Object.create(null);return K.forEach(e.toJSON(),(e,n)=>{t[n]=nn(e)}),t}var an=Symbol(`internals`);function on(e){return e&&String(e).trim().toLowerCase()}function sn(e){return e===!1||e==null?e:K.isArray(e)?e.map(sn):tn(String(e))}function cn(e){let t=Object.create(null),n=/([^\s,;=]+)\s*(?:=\s*([^,;]+))?/g,r;for(;r=n.exec(e);)t[r[1]]=r[2];return t}var ln=/^[!#$%&'*+\-.^_`|~0-9A-Za-z]+$/;function un(e){let t=0,n=e.length;for(;t<n;){let n=e.charCodeAt(t);if(n!==9&&n!==32)break;t+=1}for(;n>t;){let t=e.charCodeAt(n-1);if(t!==9&&t!==32)break;--n}return t===0&&n===e.length?e:e.slice(t,n)}function dn(e){let t=e.length-1;if(t<1||e.charCodeAt(0)!==34||e.charCodeAt(t)!==34)return e;let n=``;for(let r=1;r<t;r++){let i=e.charCodeAt(r);if(i===34||i===92&&(r+=1,r>=t))return e;n+=e[r]}return n}function fn(e){let t=Object.create(null),n=String(e),r=0,i=!1,a=!1;function o(e){let i=un(n.slice(r,e)),a=i.indexOf(`=`);if(a<1)return;let o=un(i.slice(0,a));if(!ln.test(o))return;let s=o.toLowerCase();if(s===`__proto__`||s===`constructor`||s===`prototype`)return;let c=un(i.slice(a+1));t[s]=dn(c)}for(let e=0;e<n.length;e++){let t=n.charCodeAt(e);i?a?a=!1:t===92?a=!0:t===34&&(i=!1):t===34?i=!0:(t===44||t===59)&&(o(e),r=e+1)}return o(n.length),t}var pn=e=>/^[-_a-zA-Z0-9^`|~,!#$%&'*+.]+$/.test(e.trim());function mn(e,t,n,r,i){if(K.isFunction(r))return r.call(this,t,n);if(i&&(t=n),K.isString(t)){if(K.isString(r))return t.indexOf(r)!==-1;if(K.isRegExp(r))return r.test(t)}}function hn(e){return e.trim().toLowerCase().replace(/([a-z\d])(\w*)/g,(e,t,n)=>t.toUpperCase()+n)}function gn(e,t){let n=K.toCamelCase(` `+t);[`get`,`set`,`has`].forEach(r=>{Object.defineProperty(e,r+n,{__proto__:null,value:function(e,n,i){return this[r].call(this,t,e,n,i)},configurable:!0})})}var q=class{constructor(e){e&&this.set(e)}set(e,t,n){let r=this;function i(e,t,n){let i=on(t);if(!i)return;let a=K.findKey(r,i);(!a||r[a]===void 0||n===!0||n===void 0&&r[a]!==!1)&&(r[a||t]=sn(e))}let a=(e,t)=>K.forEach(e,(e,n)=>i(e,n,t));if(K.isPlainObject(e)||e instanceof this.constructor)a(e,t);else if(K.isString(e)&&(e=e.trim())&&!pn(e))a(Xt(e),t);else if(K.isObject(e)&&K.isSafeIterable(e)){let n=Object.create(null),r,i;for(let t of e){if(!K.isArray(t))throw TypeError(`Object iterator must return a key-value pair`);i=t[0],K.hasOwnProp(n,i)?(r=n[i],n[i]=K.isArray(r)?[...r,t[1]]:[r,t[1]]):n[i]=t[1]}a(n,t)}else e!=null&&i(t,e,n);return this}get(e,t){if(e=on(e),e){let n=K.findKey(this,e);if(n){let e=this[n];if(!t)return e;if(t===!0)return cn(e);if(K.isFunction(t))return t.call(this,e,n);if(K.isRegExp(t))return t.exec(e);throw TypeError(`parser must be boolean|regexp|function`)}}}has(e,t){if(e=on(e),e){let n=K.findKey(this,e);return!!(n&&this[n]!==void 0&&(!t||mn(this,this[n],n,t)))}return!1}delete(e,t){let n=this,r=!1;function i(e){if(e=on(e),e){let i=K.findKey(n,e);i&&(!t||mn(n,n[i],i,t))&&(delete n[i],r=!0)}}return K.isArray(e)?e.forEach(i):i(e),r}clear(e){let t=Object.keys(this),n=t.length,r=!1;for(;n--;){let i=t[n];(!e||mn(this,this[i],i,e,!0))&&(delete this[i],r=!0)}return r}normalize(e){let t=this,n={};return K.forEach(this,(r,i)=>{let a=K.findKey(n,i);if(a){t[a]=sn(r),delete t[i];return}let o=e?hn(i):String(i).trim();o!==i&&delete t[i],t[o]=sn(r),n[o]=!0}),this}concat(...e){return this.constructor.concat(this,...e)}toJSON(e){let t=Object.create(null);return K.forEach(this,(n,r)=>{n!=null&&n!==!1&&(t[r]=e&&K.isArray(n)?n.join(`, `):n)}),t}[Symbol.iterator](){return Object.entries(this.toJSON())[Symbol.iterator]()}toString(){return Object.entries(this.toJSON()).map(([e,t])=>e+`: `+t).join(`
`)}getSetCookie(){let e=this.get(`set-cookie`);return K.isArray(e)?e:e==null||e===!1?[]:[e]}get[Symbol.toStringTag](){return`AxiosHeaders`}static from(e){return e instanceof this?e:new this(e)}static parseParameters(e){return fn(e)}static concat(e,...t){let n=new this(e);return t.forEach(e=>n.set(e)),n}static accessor(e){let t=(this[an]=this[an]={accessors:{}}).accessors,n=this.prototype;function r(e){let r=on(e);t[r]||(gn(n,e),t[r]=!0)}return K.isArray(e)?e.forEach(r):r(e),this}};q.accessor([`Content-Type`,`Content-Length`,`Accept`,`Accept-Encoding`,`User-Agent`,`Authorization`]),K.reduceDescriptors(q.prototype,({value:e},t)=>{let n=t[0].toUpperCase()+t.slice(1);return{get:()=>e,set(e){this[n]=e}}}),K.freezeMethods(q);var _n=`[REDACTED ****]`;function vn(e){if(K.hasOwnProp(e,`toJSON`))return!0;let t=Object.getPrototypeOf(e);for(;t&&t!==Object.prototype;){if(K.hasOwnProp(t,`toJSON`))return!0;t=Object.getPrototypeOf(t)}return!1}function yn(e,t){let n=new Set(t.map(e=>String(e).toLowerCase())),r=[],i=e=>{if(typeof e!=`object`||!e||K.isBuffer(e))return e;if(r.indexOf(e)!==-1)return;e instanceof q&&(e=e.toJSON()),r.push(e);let t;if(K.isArray(e))t=[],e.forEach((e,n)=>{let r=i(e);K.isUndefined(r)||(t[n]=r)});else{if(!K.isPlainObject(e)&&vn(e))return r.pop(),e;t=Object.create(null);for(let[r,a]of Object.entries(e)){let e=n.has(r.toLowerCase())?_n:i(a);K.isUndefined(e)||(t[r]=e)}}return r.pop(),t};return i(e)}function bn(e){try{return String(e)}catch{return``}}function xn(e){return e.errors.map(e=>{try{return e&&e.message?bn(e.message):bn(e)}catch{return``}}).filter(Boolean).join(`; `)||e.name||`AggregateError`}var J=class e extends Error{static from(t,n,r,i,a,o){let s=t.message;!s&&K.isArray(t.errors)&&t.errors.length&&(s=xn(t));let c=new e(s,n||t.code,r,i,a);return Object.defineProperty(c,"cause",{__proto__:null,value:t,writable:!0,enumerable:!1,configurable:!0}),c.name=t.name,t.status!=null&&c.status==null&&(c.status=t.status),o&&Object.assign(c,o),c}constructor(e,t,n,r,i){super(e),Object.defineProperty(this,"message",{__proto__:null,value:e,enumerable:!0,writable:!0,configurable:!0}),this.name=`AxiosError`,this.isAxiosError=!0,t&&(this.code=t),n&&(this.config=n),r&&(this.request=r),i&&(this.response=i,this.status=i.status)}toJSON(){let e=this.config,t=e&&K.hasOwnProp(e,`redact`)?e.redact:void 0,n=K.isArray(t)&&t.length>0?yn(e,t):K.toJSONObject(e);return{message:this.message,name:this.name,description:this.description,number:this.number,fileName:this.fileName,lineNumber:this.lineNumber,columnNumber:this.columnNumber,stack:this.stack,config:n,code:this.code,status:this.status}}};J.ERR_BAD_OPTION_VALUE=`ERR_BAD_OPTION_VALUE`,J.ERR_BAD_OPTION=`ERR_BAD_OPTION`,J.ECONNABORTED=`ECONNABORTED`,J.ETIMEDOUT=`ETIMEDOUT`,J.ECONNREFUSED=`ECONNREFUSED`,J.ERR_NETWORK=`ERR_NETWORK`,J.ERR_FR_TOO_MANY_REDIRECTS=`ERR_FR_TOO_MANY_REDIRECTS`,J.ERR_DEPRECATED=`ERR_DEPRECATED`,J.ERR_BAD_RESPONSE=`ERR_BAD_RESPONSE`,J.ERR_BAD_REQUEST=`ERR_BAD_REQUEST`,J.ERR_CANCELED=`ERR_CANCELED`,J.ERR_NOT_SUPPORT=`ERR_NOT_SUPPORT`,J.ERR_INVALID_URL=`ERR_INVALID_URL`,J.ERR_FORM_DATA_DEPTH_EXCEEDED=`ERR_FORM_DATA_DEPTH_EXCEEDED`;function Sn(e){return K.isPlainObject(e)||K.isArray(e)}function Cn(e){return K.endsWith(e,`[]`)?e.slice(0,-2):e}function wn(e,t,n){return e?e.concat(t).map(function(e,t){return e=Cn(e),!n&&t?`[`+e+`]`:e}).join(n?`.`:``):t}function Tn(e){return K.isArray(e)&&!e.some(Sn)}var En=K.toFlatObject(K,{},null,function(e){return/^is[A-Z]/.test(e)});function Dn(e,t,n){if(!K.isObject(e))throw TypeError(`target must be an object`);t||=new FormData,n=K.toFlatObject(n,{metaTokens:!0,dots:!1,indexes:!1},!1,function(e,t){return!K.isUndefined(t[e])});let r=n.metaTokens,i=n.visitor||m,a=n.dots,o=n.indexes,s=n.Blob||typeof Blob<`u`&&Blob,c=n.maxDepth===void 0?100:n.maxDepth,l=s&&K.isSpecCompliantForm(t),u=[];if(!K.isFunction(i))throw TypeError(`visitor must be a function`);function d(e){if(e===null)return``;if(K.isDate(e))return e.toISOString();if(K.isBoolean(e))return e.toString();if(!l&&K.isBlob(e))throw new J(`Blob is not supported. Use a Buffer instead.`);if(K.isArrayBuffer(e)||K.isTypedArray(e)){if(l&&typeof s==`function`)return new s([e]);throw new J(`Blob is not supported. Use a Buffer instead.`,J.ERR_NOT_SUPPORT)}return e}function f(e){if(e>c)throw new J(`Object is too deeply nested (`+e+` levels). Max depth: `+c,J.ERR_FORM_DATA_DEPTH_EXCEEDED)}function p(e,t){if(c===1/0)return JSON.stringify(e);let n=[];return JSON.stringify(e,function(e,r){if(!K.isObject(r))return r;for(;n.length&&n[n.length-1]!==this;)n.pop();return n.push(r),f(t+n.length-1),r})}function m(e,n,i){let s=e;if(K.isReactNative(t)&&K.isReactNativeBlob(e))return t.append(wn(i,n,a),d(e)),!1;if(e&&!i&&typeof e==`object`){if(K.endsWith(n,`{}`))n=r?n:n.slice(0,-2),e=p(e,1);else if(K.isArray(e)&&Tn(e)||(K.isFileList(e)||K.endsWith(n,`[]`))&&(s=K.toArray(e)))return n=Cn(n),s.forEach(function(e,r){!(K.isUndefined(e)||e===null)&&t.append(o===!0?wn([n],r,a):o===null?n:n+`[]`,d(e))}),!1}return Sn(e)?!0:(t.append(wn(i,n,a),d(e)),!1)}let h=Object.assign(En,{defaultVisitor:m,convertValue:d,isVisitable:Sn});function g(e,n,r=0){if(!K.isUndefined(e)){if(f(r),u.indexOf(e)!==-1)throw Error(`Circular reference detected in `+n.join(`.`));u.push(e),K.forEach(e,function(e,a){(!(K.isUndefined(e)||e===null)&&i.call(t,e,K.isString(a)?a.trim():a,n,h))===!0&&g(e,n?n.concat(a):[a],r+1)}),u.pop()}}if(!K.isObject(e))throw TypeError(`data must be an object`);return g(e),t}function On(e){let t={"!":`%21`,"'":`%27`,"(":`%28`,")":`%29`,"~":`%7E`,"%20":`+`};return encodeURIComponent(e).replace(/[!'()~]|%20/g,function(e){return t[e]})}function kn(e,t){this._pairs=[],e&&Dn(e,this,t)}var An=kn.prototype;An.append=function(e,t){this._pairs.push([e,t])},An.toString=function(e){let t=e?t=>e.call(this,t,On):On;return this._pairs.map(function(e){return t(e[0])+`=`+t(e[1])},``).join(`&`)};function jn(e){return encodeURIComponent(e).replace(/%3A/gi,`:`).replace(/%24/g,`$`).replace(/%2C/gi,`,`).replace(/%20/g,`+`)}function Mn(e,t,n){if(!t)return e;e||=``;let r=K.isFunction(n)?{serialize:n}:n,i=K.getSafeProp(r,`encode`)||jn,a=K.getSafeProp(r,`serialize`),o;if(o=a?a(t,r):K.isURLSearchParams(t)?t.toString():new kn(t,r).toString(i),o){let t=e.indexOf(`#`);t!==-1&&(e=e.slice(0,t)),e+=(e.indexOf(`?`)===-1?`?`:`&`)+o}return e}var Nn=class{constructor(){this.handlers=[]}use(e,t,n){return this.handlers.push({fulfilled:e,rejected:t,synchronous:n?n.synchronous:!1,runWhen:n?n.runWhen:null}),this.handlers.length-1}eject(e){this.handlers[e]&&(this.handlers[e]=null)}clear(){this.handlers&&=[]}forEach(e){K.forEach(this.handlers,function(t){t!==null&&e(t)})}},Pn={silentJSONParsing:!0,forcedJSONParsing:!0,clarifyTimeoutError:!1,legacyInterceptorReqResOrdering:!0,advertiseZstdAcceptEncoding:!1,validateStatusUndefinedResolves:!0},Fn={isBrowser:!0,classes:{URLSearchParams:typeof URLSearchParams<`u`?URLSearchParams:kn,FormData:typeof FormData<`u`?FormData:null,Blob:typeof Blob<`u`?Blob:null},protocols:[`http`,`https`,`file`,`blob`,`url`,`data`]},In=r({hasBrowserEnv:()=>Ln,hasStandardBrowserEnv:()=>zn,hasStandardBrowserWebWorkerEnv:()=>Bn,navigator:()=>Rn,origin:()=>Vn}),Ln=typeof window<`u`&&typeof document<`u`,Rn=typeof navigator==`object`&&navigator||void 0,zn=Ln&&(!Rn||[`ReactNative`,`NativeScript`,`NS`].indexOf(Rn.product)<0),Bn=typeof WorkerGlobalScope<`u`&&self instanceof WorkerGlobalScope&&typeof self.importScripts==`function`,Vn=Ln&&window.location.href||`http://localhost`,Y={...In,...Fn};function Hn(e,t){return Dn(e,new Y.classes.URLSearchParams,{visitor:function(e,t,n,r){return Y.isNode&&K.isBuffer(e)?(this.append(t,e.toString(`base64`)),!1):r.defaultVisitor.apply(this,arguments)},...t})}var Un=100;function Wn(e){if(e>Un)throw new J(`FormData field is too deeply nested (`+e+` levels). Max depth: `+Un,J.ERR_FORM_DATA_DEPTH_EXCEEDED)}function Gn(e){let t=[],n=/[^.[\]]+|\[([^.[\]]*)]/g,r;for(;(r=n.exec(e))!==null;)Wn(t.length),t.push(r[0]===`[]`?``:r[1]||r[0]);return t}function Kn(e){let t={},n=Object.keys(e),r,i=n.length,a;for(r=0;r<i;r++)a=n[r],t[a]=e[a];return t}function qn(e){function t(e,n,r,i){Wn(i);let a=e[i++];if(a===`__proto__`)return!0;let o=Number.isFinite(+a),s=i>=e.length;return a=!a&&K.isArray(r)?r.length:a,s?(K.hasOwnProp(r,a)?r[a]=K.isArray(r[a])?r[a].concat(n):[r[a],n]:r[a]=n,!o):((!K.hasOwnProp(r,a)||!K.isObject(r[a]))&&(r[a]=[]),t(e,n,r[a],i)&&K.isArray(r[a])&&(r[a]=Kn(r[a])),!o)}if(K.isFormData(e)&&K.isFunction(e.entries)){let n={};return K.forEachEntry(e,(e,r)=>{t(Gn(e),r,n,0)}),n}return null}var Jn=(e,t)=>e!=null&&K.hasOwnProp(e,t)?e[t]:void 0;function Yn(e,t,n){if(K.isString(e))try{return(t||JSON.parse)(e),K.trim(e)}catch(e){if(e.name!==`SyntaxError`)throw e}return(n||JSON.stringify)(e)}var Xn={transitional:Pn,adapter:[`xhr`,`http`,`fetch`],transformRequest:[function(e,t){let n=t.getContentType()||``,r=n.indexOf(`application/json`)>-1,i=K.isObject(e);if(i&&K.isHTMLForm(e)&&(e=new FormData(e)),K.isFormData(e))return r?JSON.stringify(qn(e)):e;if(K.isArrayBuffer(e)||K.isBuffer(e)||K.isStream(e)||K.isFile(e)||K.isBlob(e)||K.isReadableStream(e))return e;if(K.isArrayBufferView(e))return e.buffer;if(K.isURLSearchParams(e))return t.setContentType(`application/x-www-form-urlencoded;charset=utf-8`,!1),e.toString();let a;if(i){let t=Jn(this,`formSerializer`);if(n.indexOf(`application/x-www-form-urlencoded`)>-1)return Hn(e,t).toString();if((a=K.isFileList(e))||n.indexOf(`multipart/form-data`)>-1){let n=Jn(this,`env`),r=n&&n.FormData;return Dn(a?{"files[]":e}:e,r&&new r,t)}}return i||r?(t.setContentType(`application/json`,!1),Yn(e)):e}],transformResponse:[function(e){let t=Jn(this,`transitional`)||Xn.transitional,n=t&&t.forcedJSONParsing,r=Jn(this,`responseType`),i=r===`json`;if(K.isResponse(e)||K.isReadableStream(e))return e;if(e&&K.isString(e)&&(n&&!r||i)){let n=!(t&&t.silentJSONParsing)&&i;try{return JSON.parse(e,Jn(this,`parseReviver`))}catch(e){if(n)throw e.name===`SyntaxError`?J.from(e,J.ERR_BAD_RESPONSE,this,null,Jn(this,`response`)):e}}return e}],timeout:0,xsrfCookieName:`XSRF-TOKEN`,xsrfHeaderName:`X-XSRF-TOKEN`,maxContentLength:-1,maxBodyLength:-1,env:{FormData:Y.classes.FormData,Blob:Y.classes.Blob},validateStatus:function(e){return e>=200&&e<300},headers:{common:{Accept:`application/json, text/plain, */*`,"Content-Type":void 0}}};K.forEach([`delete`,`get`,`head`,`post`,`put`,`patch`,`query`],e=>{Xn.headers[e]={}});function Zn(e,t){let n=this||Xn,r=t||n,i=q.from(r.headers),a=r.data;return K.forEach(e,function(e){a=e.call(n,a,i.normalize(),t?t.status:void 0)}),i.normalize(),a}function Qn(e){return!!(e&&e.__CANCEL__)}var $n=class extends J{constructor(e,t,n){super(e??`canceled`,J.ERR_CANCELED,t,n),this.name=`CanceledError`,this.__CANCEL__=!0}};function er(e,t,n){let r=n.config.validateStatus;!n.status||!r||r(n.status)?e(n):t(new J(`Request failed with status code `+n.status,n.status>=400&&n.status<500?J.ERR_BAD_REQUEST:J.ERR_BAD_RESPONSE,n.config,n.request,n))}function tr(e){let t=/^([-+\w]{1,25}):(?:\/\/)?/.exec(e);return t&&t[1]||``}function nr(e,t){e||=10;let n=Array(e),r=Array(e),i=0,a=0,o;return t=t===void 0?1e3:t,function(s){let c=Date.now(),l=r[a];o||=c,n[i]=s,r[i]=c;let u=a,d=0;for(;u!==i;)d+=n[u++],u%=e;if(i=(i+1)%e,i===a&&(a=(a+1)%e),c-o<t)return;let f=l&&c-l;return f?Math.round(d*1e3/f):void 0}}function rr(e,t){let n=0,r=1e3/t,i,a,o=(t,r=Date.now())=>{n=r,i=null,a&&=(clearTimeout(a),null),e(...t)};return[(...e)=>{let t=Date.now(),s=t-n;s>=r?o(e,t):(i=e,a||=setTimeout(()=>{a=null,o(i)},r-s))},()=>i&&o(i)]}var ir=(e,t,n=3)=>{let r=0,i=nr(50,250);return rr(n=>{if(!n||typeof n.loaded!=`number`)return;let a=n.loaded,o=n.lengthComputable?n.total:void 0,s=Math.max(0,o==null?a:Math.min(a,o)),c=Math.max(0,s-r),l=i(c);r=Math.max(r,s),e({loaded:s,total:o,progress:o?s/o:void 0,bytes:c,rate:l||void 0,estimated:l&&o?(o-s)/l:void 0,event:n,lengthComputable:o!=null,[t?`download`:`upload`]:!0})},n)},ar=(e,t)=>{let n=e!=null;return[r=>t[0]({lengthComputable:n,total:e,loaded:r}),t[1]]},or=(e,t=K.asap)=>(...n)=>t(()=>e(...n)),sr=Y.hasStandardBrowserEnv?((e,t)=>n=>(n=new URL(n,Y.origin),e.protocol===n.protocol&&e.host===n.host&&(t||e.port===n.port)))(new URL(Y.origin),Y.navigator&&/(msie|trident)/i.test(Y.navigator.userAgent)):()=>!0,cr=Y.hasStandardBrowserEnv?{write(e,t,n,r,i,a,o){if(typeof document>`u`)return;let s=[`${e}=${encodeURIComponent(t)}`];K.isNumber(n)&&s.push(`expires=${new Date(n).toUTCString()}`),K.isString(r)&&s.push(`path=${r}`),K.isString(i)&&s.push(`domain=${i}`),a===!0&&s.push(`secure`),K.isString(o)&&s.push(`SameSite=${o}`),document.cookie=s.join(`; `)},read(e){if(typeof document>`u`)return null;let t=document.cookie.split(`;`);for(let n=0;n<t.length;n++){let r=t[n].replace(/^\s+/,``),i=r.indexOf(`=`);if(i!==-1&&r.slice(0,i)===e)try{return decodeURIComponent(r.slice(i+1))}catch{return r.slice(i+1)}}return null},remove(e){this.write(e,``,Date.now()-864e5,`/`)}}:{write(){},read(){return null},remove(){}};function lr(e){return typeof e==`string`&&/^([a-z][a-z\d+\-.]*:)?\/\//i.test(e)}function ur(e,t){if(!t)return e;let n=e.length;for(;n>0&&e.charCodeAt(n-1)===47;)n--;return e.slice(0,n)+`/`+t.replace(/^\/+/,``)}var dr=/^https?:(?!\/\/)/i,fr=/[\t\n\r]/g;function pr(e){let t=0;for(;t<e.length&&e.charCodeAt(t)<=32;)t++;return e.slice(t)}function mr(e){return pr(e).replace(fr,``)}function hr(e){return e&&e.replace(/(^|&)([^=&]*=)?[^&]+/g,(e,t,n=``)=>`${t}${n}${_n}`)}function gr(e){let t=e.replace(/^(https?:\/{0,2})[^/?#]*@/i,`$1${_n}@`),n=t.indexOf(`#`),r=(n===-1?t:t.slice(0,n)).replace(/([?&][^=&#]*=)[^&#]*/g,`$1${_n}`);return n===-1?r:`${r}#${hr(t.slice(n+1))}`}function _r(e,t){if(typeof e==`string`){let n=mr(e);if(dr.test(n))throw new J(`Invalid URL ${JSON.stringify(gr(n))}: missing "//" after protocol`,J.ERR_INVALID_URL,t)}}function vr(e,t,n,r){_r(t,r);let i=!lr(t);return e&&(i||n===!1)?(_r(e,r),ur(e,t)):t}var yr=e=>e instanceof q?{...e}:e,br=e=>Object.getOwnPropertySymbols&&Object.getOwnPropertyDescriptor?Object.keys(e).concat(Object.getOwnPropertySymbols(e).filter(t=>Object.getOwnPropertyDescriptor(e,t).enumerable)):Object.keys(e);function X(e,t){e||={},t||={};let n=Object.create(null);Object.defineProperty(n,"hasOwnProperty",{__proto__:null,value:Object.prototype.hasOwnProperty,enumerable:!1,writable:!0,configurable:!0});function r(e,t,n,r){return K.isPlainObject(e)&&K.isPlainObject(t)?K.merge.call({caseless:r},e,t):K.isPlainObject(t)?K.merge({},t):K.isArray(t)?t.slice():t}function i(e,t,n,i){if(!K.isUndefined(t))return r(e,t,n,i);if(!K.isUndefined(e))return r(void 0,e,n,i)}function a(e,t){if(!K.isUndefined(t))return r(void 0,t)}function o(e,t){if(!K.isUndefined(t))return r(void 0,t);if(!K.isUndefined(e))return r(void 0,e)}function s(n){let r=K.hasOwnProp(t,`transitional`)?t.transitional:void 0;if(!K.isUndefined(r))if(K.isPlainObject(r)){if(K.hasOwnProp(r,n))return r[n]}else return;let i=K.hasOwnProp(e,`transitional`)?e.transitional:void 0;if(K.isPlainObject(i)&&K.hasOwnProp(i,n))return i[n]}function c(n,i,a){if(K.hasOwnProp(t,a))return r(n,i);if(K.hasOwnProp(e,a))return r(void 0,n)}let l={url:a,method:a,data:a,baseURL:o,transformRequest:o,transformResponse:o,paramsSerializer:o,timeout:o,timeoutMessage:o,withCredentials:o,withXSRFToken:o,adapter:o,responseType:o,xsrfCookieName:o,xsrfHeaderName:o,onUploadProgress:o,onDownloadProgress:o,decompress:o,maxContentLength:o,maxBodyLength:o,beforeRedirect:o,transport:o,httpAgent:o,httpsAgent:o,cancelToken:o,socketPath:o,allowedSocketPaths:o,responseEncoding:o,validateStatus:c,headers:(e,t,n)=>i(yr(e),yr(t),n,!0)};return K.forEach(br({...e,...t}),function(r){if(r===`__proto__`||r===`constructor`||r===`prototype`)return;let a=K.hasOwnProp(l,r)?l[r]:i,o=a(K.hasOwnProp(e,r)?e[r]:void 0,K.hasOwnProp(t,r)?t[r]:void 0,r);K.isUndefined(o)&&a!==c||(n[r]=o)}),K.hasOwnProp(t,`validateStatus`)&&K.isUndefined(t.validateStatus)&&s(`validateStatusUndefinedResolves`)===!1&&(K.hasOwnProp(e,`validateStatus`)?n.validateStatus=r(void 0,e.validateStatus):delete n.validateStatus),n}var xr=[`content-type`,`content-length`];function Sr(e,t,n){if(n!==`content-only`){e.set(t);return}Object.entries(t||{}).forEach(([t,n])=>{xr.includes(t.toLowerCase())&&e.set(t,n)})}var Cr=e=>encodeURIComponent(e).replace(/%([0-9A-F]{2})/gi,(e,t)=>String.fromCharCode(parseInt(t,16)));function wr(e){let t=X({},e),n=e=>K.hasOwnProp(t,e)?t[e]:void 0,r=n(`data`),i=n(`withXSRFToken`),a=n(`xsrfHeaderName`),o=n(`xsrfCookieName`),s=n(`headers`),c=n(`auth`),l=n(`baseURL`),u=n(`allowAbsoluteUrls`),d=n(`url`);if(t.headers=s=q.from(s),t.url=Mn(vr(l,d,u,t),n(`params`),n(`paramsSerializer`)),c){let t=K.getSafeProp(c,`username`)||``,n=K.getSafeProp(c,`password`)||``;try{s.set(`Authorization`,`Basic `+btoa(t+`:`+(n?Cr(n):``)))}catch(t){throw J.from(t,J.ERR_BAD_OPTION_VALUE,e)}}if(K.isFormData(r)&&(Y.hasStandardBrowserEnv||Y.hasStandardBrowserWebWorkerEnv||K.isReactNative(r)?s.setContentType(void 0):K.isFunction(r.getHeaders)&&Sr(s,r.getHeaders(),n(`formDataHeaderPolicy`))),Y.hasStandardBrowserEnv&&(K.isFunction(i)&&(i=i(t)),i===!0||i==null&&sr(t.url))){let e=a&&o&&cr.read(o);e&&s.set(a,e)}return t}var Tr=typeof XMLHttpRequest<`u`&&function(e){return new Promise(function(t,n){let r=wr(e),i=r.data,a=q.from(r.headers).normalize(),{responseType:o,onUploadProgress:s,onDownloadProgress:c}=r,l,u,d,f,p;function m(){f&&f(),p&&p(),r.cancelToken&&r.cancelToken.unsubscribe(l),r.signal&&r.signal.removeEventListener(`abort`,l)}let h=new XMLHttpRequest;h.open(r.method.toUpperCase(),r.url,!0),h.timeout=r.timeout;function g(){if(!h)return;let r=q.from(`getAllResponseHeaders`in h&&h.getAllResponseHeaders());er(function(e){t(e),m()},function(e){n(e),m()},{data:!o||o===`text`||o===`json`?h.responseText:h.response,status:h.status,statusText:h.statusText,headers:r,config:e,request:h}),h=null}`onloadend`in h?h.onloadend=g:h.onreadystatechange=function(){!h||h.readyState!==4||h.status===0&&!(h.responseURL&&h.responseURL.startsWith(`file:`))||setTimeout(g)},h.onabort=function(){h&&=(n(new J(`Request aborted`,J.ECONNABORTED,e,h)),m(),null)},h.onerror=function(t){let r=new J(t&&t.message?t.message:`Network Error`,J.ERR_NETWORK,e,h);r.event=t||null,n(r),m(),h=null},h.ontimeout=function(){let t=r.timeout?`timeout of `+r.timeout+`ms exceeded`:`timeout exceeded`,i=r.transitional||Pn;r.timeoutErrorMessage&&(t=r.timeoutErrorMessage),n(new J(t,i.clarifyTimeoutError?J.ETIMEDOUT:J.ECONNABORTED,e,h)),m(),h=null},i===void 0&&a.setContentType(null),`setRequestHeader`in h&&K.forEach(rn(a),function(e,t){h.setRequestHeader(t,e)}),K.isUndefined(r.withCredentials)||(h.withCredentials=!!r.withCredentials),o&&o!==`json`&&(h.responseType=r.responseType),c&&([d,p]=ir(c,!0),h.addEventListener(`progress`,d)),s&&h.upload&&([u,f]=ir(s),h.upload.addEventListener(`progress`,u),h.upload.addEventListener(`loadend`,f)),(r.cancelToken||r.signal)&&(l=t=>{h&&=(n(!t||t.type?new $n(null,e,h):t),h.abort(),m(),null)},r.cancelToken&&r.cancelToken.subscribe(l),r.signal&&(r.signal.aborted?l():r.signal.addEventListener(`abort`,l)));let _=tr(r.url);if(_&&!Y.protocols.includes(_)){n(new J(`Unsupported protocol `+_+`:`,J.ERR_BAD_REQUEST,e)),m();return}h.send(i||null)})},Er=(e,t)=>{if(e=e?e.filter(Boolean):[],!t&&!e.length)return;let n=new AbortController,r=!1,i=function(e){if(!r){r=!0,o();let t=e instanceof Error?e:this.reason;n.abort(t instanceof J?t:new $n(t instanceof Error?t.message:t))}},a=t&&setTimeout(()=>{a=null,i(new J(`timeout of ${t}ms exceeded`,J.ETIMEDOUT))},t),o=()=>{e&&=(a&&clearTimeout(a),a=null,e.forEach(e=>{e.unsubscribe?e.unsubscribe(i):e.removeEventListener(`abort`,i)}),null)};e.forEach(e=>{if(!r){if(e.aborted){i.call(e);return}e.addEventListener(`abort`,i,{once:!0})}});let{signal:s}=n;return s.unsubscribe=()=>K.asap(o),s},Dr=function*(e,t){let n=e.byteLength;if(!t||n<t){yield e;return}let r=0,i;for(;r<n;)i=r+t,yield e.slice(r,i),r=i},Or=async function*(e,t){for await(let n of kr(e))yield*Dr(n,t)},kr=async function*(e){if(e[Symbol.asyncIterator]){yield*e;return}let t=e.getReader();try{for(;;){let{done:e,value:n}=await t.read();if(e)break;yield n}}finally{await t.cancel()}},Ar=(e,t,n,r)=>{let i=Or(e,t),a=0,o,s=e=>{o||(o=!0,r&&r(e))};return new ReadableStream({async pull(e){try{let{done:t,value:r}=await i.next();if(t){s(),e.close();return}let o=r.byteLength;n&&n(a+=o),e.enqueue(new Uint8Array(r))}catch(e){throw s(e),e}},cancel(e){return s(e),i.return()}},{highWaterMark:2})},jr=e=>e>=48&&e<=57||e>=65&&e<=70||e>=97&&e<=102,Mr=(e,t,n)=>t+2<n&&jr(e.charCodeAt(t+1))&&jr(e.charCodeAt(t+2)),Nr=e=>e<=57?e-48:(e&223)-55,Pr=e=>e>=65&&e<=90||e>=97&&e<=122||e>=48&&e<=57||e===43||e===47||e===45||e===95,Fr=e=>e===9||e===10||e===12||e===13||e===32,Ir=e=>{let t=Math.floor(e/4),n=e%4;return t*3+(n===2?1:n===3?2:0)},Lr=e=>{let t=e.length,n=0;return t>0&&e.charCodeAt(t-1)===61&&(n++,t>1&&e.charCodeAt(t-2)===61&&n++),Math.floor((t-n)*3/4)},Rr=e=>{let t=e.length,n=0,r=0,i=!1;for(let a=0;a<t;a++){let o=e.charCodeAt(a);if(o===37&&Mr(e,a,t)&&(o=Nr(e.charCodeAt(a+1))*16+Nr(e.charCodeAt(a+2)),a+=2),!Fr(o)){if(o===61){r++;continue}if(!Pr(o)||r>0){i=!0;continue}n++}}return i||r>2||r>0&&(n+r)%4!=0||n%4==1?Lr(e):Ir(n)},zr=(e,t)=>{if(!e||typeof e!=`string`||!e.startsWith(`data:`))return 0;let n=e.indexOf(`,`);if(n<0)return 0;let r=e.slice(5,n),i=e.slice(n+1);if(/;base64/i.test(r))return t(i);let a=0;for(let e=0,t=i.length;e<t;e++){let n=i.charCodeAt(e);if(n===37&&Mr(i,e,t))a+=1,e+=2;else if(n<128)a+=1;else if(n<2048)a+=2;else if(n>=55296&&n<=56319&&e+1<t){let t=i.charCodeAt(e+1);t>=56320&&t<=57343?(a+=4,e++):a+=3}else a+=3}return a};function Br(e){let t=typeof e==`string`?e.indexOf(`#`):-1;return zr(t===-1?e:e.slice(0,t),Rr)}var Vr=`1.19.0`,Hr=65536,{isFunction:Ur}=K,Wr=e=>encodeURIComponent(e).replace(/%([0-9A-F]{2})/gi,(e,t)=>String.fromCharCode(parseInt(t,16))),Gr=e=>{if(!K.isString(e))return e;try{return decodeURIComponent(e)}catch{return e}},Kr=(e,...t)=>{try{return!!e(...t)}catch{return!1}},qr=e=>{let t=e.indexOf(`://`),n=e;return t!==-1&&(n=n.slice(t+3)),n.includes(`@`)||n.includes(`:`)},Jr=e=>{let t=K.global!==void 0&&K.global!==null?K.global:globalThis,{ReadableStream:n,TextEncoder:r}=t;e=K.merge.call({skipUndefined:!0},{Request:t.Request,Response:t.Response},e);let{fetch:i,Request:a,Response:o}=e,s=i?Ur(i):typeof fetch==`function`,c=Ur(a),l=Ur(o);if(!s)return!1;let u=s&&Ur(n),d=s&&(typeof r==`function`?(e=>t=>e.encode(t))(new r):async e=>new Uint8Array(await new a(e).arrayBuffer())),f=c&&u&&Kr(()=>{let e=!1,t=new a(Y.origin,{body:new n,method:`POST`,get duplex(){return e=!0,`half`}}),r=t.headers.has(`Content-Type`);return t.body!=null&&t.body.cancel(),e&&!r}),p=l&&u&&Kr(()=>K.isReadableStream(new o(``).body)),m={stream:p&&(e=>e.body)};s&&[`text`,`arrayBuffer`,`blob`,`formData`,`stream`].forEach(e=>{!m[e]&&(m[e]=(t,n)=>{let r=t&&t[e];if(r)return r.call(t);throw new J(`Response type '${e}' is not supported`,J.ERR_NOT_SUPPORT,n)})});let h=async e=>{if(e==null)return 0;if(K.isBlob(e))return e.size;if(K.isSpecCompliantForm(e))return(await new a(Y.origin,{method:`POST`,body:e}).arrayBuffer()).byteLength;if(K.isArrayBufferView(e)||K.isArrayBuffer(e))return e.byteLength;if(K.isURLSearchParams(e)&&(e+=``),K.isString(e))return(await d(e)).byteLength},g=async(e,t)=>K.toFiniteNumber(e.getContentLength())??h(t);return async e=>{let{url:t,method:n,data:s,signal:l,cancelToken:d,timeout:_,onDownloadProgress:v,onUploadProgress:y,responseType:b,headers:x,withCredentials:S=`same-origin`,fetchOptions:C,maxContentLength:w,maxBodyLength:T}=wr(e),E=K.isNumber(w)&&w>-1,D=K.isNumber(T)&&T>-1,ee=t=>K.hasOwnProp(e,t)?e[t]:void 0,te=i||fetch;b=b?(b+``).toLowerCase():`text`;let O=Er([l,d&&d.toAbortSignal()],_),k=null,A=O&&O.unsubscribe&&(()=>{O.unsubscribe()}),j,M=null,N=()=>new J(`Request body larger than maxBodyLength limit`,J.ERR_BAD_REQUEST,e,k);try{let i,l=ee(`auth`);if(l&&(i={username:K.getSafeProp(l,`username`)||``,password:K.getSafeProp(l,`password`)||``}),qr(t)){let e=new URL(t,Y.origin);!i&&(e.username||e.password)&&(i={username:Gr(e.username),password:Gr(e.password)}),(e.username||e.password)&&(e.username=``,e.password=``,t=e.href)}if(i&&(x.delete(`authorization`),x.set(`Authorization`,`Basic `+btoa(Wr((i.username||``)+`:`+(i.password||``))))),E&&typeof t==`string`&&t.startsWith(`data:`)&&Br(t)>w)throw new J(`maxContentLength size of `+w+` exceeded`,J.ERR_BAD_RESPONSE,e,k);if(D&&n!==`get`&&n!==`head`){let e=await h(s);if(typeof e==`number`&&isFinite(e)&&(j=e,e>T))throw N()}let d=D&&(K.isReadableStream(s)||K.isStream(s)),_=(e,t,n)=>Ar(e,Hr,e=>{if(D&&e>T)throw M=N();t&&t(e)},n);if(f&&n!==`get`&&n!==`head`&&(y||d)){if(j??=await g(x,s),j!==0||d){let e=new a(t,{method:`POST`,body:s,duplex:`half`}),n;if(K.isFormData(s)&&(n=e.headers.get(`content-type`))&&x.setContentType(n),e.body){let[t,n]=y&&ar(j,ir(or(y)))||[];s=_(e.body,t,n)}}}else if(d&&!c&&u&&n!==`get`&&n!==`head`)s=_(s);else if(d&&c&&!f&&n!==`get`&&n!==`head`)throw new J(`Stream request bodies are not supported by the current fetch implementation`,J.ERR_NOT_SUPPORT,e,k);K.isString(S)||(S=S?`include`:`omit`);let ne=c&&`credentials`in a.prototype;if(K.isFormData(s)){let e=x.getContentType();e&&/^multipart\/form-data/i.test(e)&&!/boundary=/i.test(e)&&x.delete(`content-type`)}x.set(`User-Agent`,`axios/`+Vr,!1);let re={...C,signal:O,method:n.toUpperCase(),headers:rn(x.normalize()),body:s,duplex:`half`,credentials:ne?S:void 0};k=c&&new a(t,re);let P=await(c?te(k,C):te(t,re)),F=q.from(P.headers);if(E){let t=K.toFiniteNumber(F.getContentLength());if(t!=null&&t>w)throw new J(`maxContentLength size of `+w+` exceeded`,J.ERR_BAD_RESPONSE,e,k)}let I=p&&(b===`stream`||b===`response`);if(p&&P.body&&(v||E||I&&A)){let t={};[`status`,`statusText`,`headers`].forEach(e=>{t[e]=P[e]});let n=K.toFiniteNumber(F.getContentLength()),[r,i]=v&&ar(n,ir(or(v),!0))||[],a=0;P=new o(Ar(P.body,Hr,t=>{if(E&&(a=t,a>w))throw new J(`maxContentLength size of `+w+` exceeded`,J.ERR_BAD_RESPONSE,e,k);r&&r(t)},()=>{i&&i(),A&&A()}),t)}b||=`text`;let L=await m[K.findKey(m,b)||`text`](P,e);if(E&&!p&&!I){let t;if(L!=null&&(typeof L.byteLength==`number`?t=L.byteLength:typeof L.size==`number`?t=L.size:typeof L==`string`&&(t=typeof r==`function`?new r().encode(L).byteLength:L.length)),typeof t==`number`&&t>w)throw new J(`maxContentLength size of `+w+` exceeded`,J.ERR_BAD_RESPONSE,e,k)}return!I&&A&&A(),await new Promise((t,n)=>{er(t,n,{data:L,headers:q.from(P.headers),status:P.status,statusText:P.statusText,config:e,request:k})})}catch(t){if(A&&A(),O&&O.aborted&&O.reason instanceof J){let n=O.reason;throw n.config=e,k&&(n.request=k),t!==n&&Object.defineProperty(n,"cause",{__proto__:null,value:t,writable:!0,enumerable:!1,configurable:!0}),n}if(M)throw k&&!M.request&&(M.request=k),M;if(t instanceof J)throw k&&!t.request&&(t.request=k),t;if(t&&t.name===`TypeError`&&/Load failed|fetch/i.test(t.message)){let n=new J(`Network Error`,J.ERR_NETWORK,e,k,t&&t.response);throw Object.defineProperty(n,"cause",{__proto__:null,value:t.cause||t,writable:!0,enumerable:!1,configurable:!0}),n}throw J.from(t,t&&t.code,e,k,t&&t.response)}}},Yr=new Map,Xr=e=>{let t=e&&e.env||{},{fetch:n,Request:r,Response:i}=t,a=[r,i,n],o=a.length,s,c,l=Yr;for(;o--;)s=a[o],c=l.get(s),c===void 0&&l.set(s,c=o?new Map:Jr(t)),l=c;return c};Xr();var Zr={http:null,xhr:Tr,fetch:{get:Xr}};K.forEach(Zr,(e,t)=>{if(e){try{Object.defineProperty(e,"name",{__proto__:null,value:t})}catch{}Object.defineProperty(e,"adapterName",{__proto__:null,value:t})}});var Qr=e=>`- ${e}`,$r=e=>K.isFunction(e)||e===null||e===!1;function ei(e,t){e=K.isArray(e)?e:[e];let{length:n}=e,r,i,a={};for(let o=0;o<n;o++){r=e[o];let n;if(i=r,!$r(r)&&(i=Zr[(n=String(r)).toLowerCase()],i===void 0))throw new J(`Unknown adapter '${n}'`);if(i&&(K.isFunction(i)||(i=i.get(t))))break;a[n||`#`+o]=i}if(!i){let e=Object.entries(a).map(([e,t])=>`adapter ${e} `+(t===!1?`is not supported by the environment`:`is not available in the build`));throw new J(`There is no suitable adapter to dispatch the request `+(n?e.length>1?`since :
`+e.map(Qr).join(`
`):` `+Qr(e[0]):`as no adapter specified`),J.ERR_NOT_SUPPORT)}return i}var ti={getAdapter:ei,adapters:Zr};function ni(e){if(e.cancelToken&&e.cancelToken.throwIfRequested(),e.signal&&e.signal.aborted)throw new $n(null,e)}function ri(e){return ni(e),e.headers=q.from(e.headers),e.data=Zn.call(e,e.transformRequest),[`post`,`put`,`patch`].indexOf(e.method)!==-1&&e.headers.setContentType(`application/x-www-form-urlencoded`,!1),ti.getAdapter(e.adapter||Xn.adapter,e)(e).then(function(t){ni(e),e.response=t;try{t.data=Zn.call(e,e.transformResponse,t)}finally{delete e.response}return t.headers=q.from(t.headers),t},function(t){if(!Qn(t)&&(ni(e),t&&t.response)){e.response=t.response;try{t.response.data=Zn.call(e,e.transformResponse,t.response)}finally{delete e.response}t.response.headers=q.from(t.response.headers)}return Promise.reject(t)})}var ii={};[`object`,`boolean`,`number`,`function`,`string`,`symbol`].forEach((e,t)=>{ii[e]=function(n){return typeof n===e||`a`+(t<1?`n `:` `)+e}});var ai={};ii.transitional=function(e,t,n){function r(e,t){return`[Axios v`+Vr+`] Transitional option '`+e+`'`+t+(n?`. `+n:``)}return(n,i,a)=>{if(e===!1)throw new J(r(i,` has been removed`+(t?` in `+t:``)),J.ERR_DEPRECATED);return t&&!ai[i]&&(ai[i]=!0,console.warn(r(i,` has been deprecated since v`+t+` and will be removed in the near future`))),!e||e(n,i,a)}},ii.spelling=function(e){return(t,n)=>(console.warn(`${n} is likely a misspelling of ${e}`),!0)};function oi(e,t,n){if(typeof e!=`object`||!e)throw new J(`options must be an object`,J.ERR_BAD_OPTION_VALUE);let r=Object.keys(e),i=r.length;for(;i-->0;){let a=r[i],o=Object.prototype.hasOwnProperty.call(t,a)?t[a]:void 0;if(o){let t=e[a],n=t===void 0||o(t,a,e);if(n!==!0)throw new J(`option `+a+` must be `+n,J.ERR_BAD_OPTION_VALUE);continue}if(n!==!0)throw new J(`Unknown option `+a,J.ERR_BAD_OPTION)}}var si={assertOptions:oi,validators:ii},Z=si.validators,Q=class{constructor(e){this.defaults=e||{},this.interceptors={request:new Nn,response:new Nn}}async request(e,t){try{return await this._request(e,t)}catch(e){if(e instanceof Error){let t={};Error.captureStackTrace?Error.captureStackTrace(t):t=Error();let n=(()=>{if(!t.stack)return``;let e=t.stack.indexOf(`
`);return e===-1?``:t.stack.slice(e+1)})();try{if(!e.stack)e.stack=n;else if(n){let t=n.indexOf(`
`),r=t===-1?-1:n.indexOf(`
`,t+1),i=r===-1?``:n.slice(r+1);String(e.stack).endsWith(i)||(e.stack+=`
`+n)}}catch{}}throw e}}_request(e,t){typeof e==`string`?(t||={},t.url=e):t=e||{},t=X(this.defaults,t);let{transitional:n,paramsSerializer:r,headers:i}=t;n!==void 0&&si.assertOptions(n,{silentJSONParsing:Z.transitional(Z.boolean),forcedJSONParsing:Z.transitional(Z.boolean),clarifyTimeoutError:Z.transitional(Z.boolean),legacyInterceptorReqResOrdering:Z.transitional(Z.boolean),advertiseZstdAcceptEncoding:Z.transitional(Z.boolean),validateStatusUndefinedResolves:Z.transitional(Z.boolean)},!1),r!=null&&(K.isFunction(r)?t.paramsSerializer={serialize:r}:si.assertOptions(r,{encode:Z.function,serialize:Z.function},!0)),t.allowAbsoluteUrls!==void 0||(this.defaults.allowAbsoluteUrls===void 0?t.allowAbsoluteUrls=!0:t.allowAbsoluteUrls=this.defaults.allowAbsoluteUrls),si.assertOptions(t,{baseUrl:Z.spelling(`baseURL`),withXsrfToken:Z.spelling(`withXSRFToken`)},!0),t.method=(t.method||this.defaults.method||`get`).toLowerCase();let a=i&&K.merge(i.common,i[t.method]);i&&K.forEach([`delete`,`get`,`head`,`post`,`put`,`patch`,`query`,`common`],e=>{delete i[e]}),t.headers=q.concat(a,i);let o=[],s=!0;this.interceptors.request.forEach(function(e){if(typeof e.runWhen==`function`&&e.runWhen(t)===!1)return;s&&=e.synchronous;let n=t.transitional||Pn;n&&n.legacyInterceptorReqResOrdering?o.unshift(e.fulfilled,e.rejected):o.push(e.fulfilled,e.rejected)});let c=[];this.interceptors.response.forEach(function(e){c.push(e.fulfilled,e.rejected)});let l,u=0,d;if(!s){let e=[ri.bind(this),void 0];for(e.unshift(...o),e.push(...c),d=e.length,l=Promise.resolve(t);u<d;)l=l.then(e[u++],e[u++]);return l}d=o.length;let f=t;for(;u<d;){let e=o[u++],t=o[u++];try{f=e?e(f):f}catch(e){if(!t){l=Promise.reject(e);break}try{let n=t.call(this,e);K.isThenable(n)&&(l=Promise.resolve(n).then(()=>ri.call(this,f)))}catch(e){l=Promise.reject(e)}break}}if(!l)try{l=ri.call(this,f)}catch(e){l=Promise.reject(e)}for(u=0,d=c.length;u<d;)l=l.then(c[u++],c[u++]);return l}getUri(e){return e=X(this.defaults,e),Mn(vr(e.baseURL,e.url,e.allowAbsoluteUrls,e),e.params,e.paramsSerializer)}};K.forEach([`delete`,`get`,`head`,`options`],function(e){Q.prototype[e]=function(t,n){return this.request(X(n||{},{method:e,url:t,data:n&&K.hasOwnProp(n,`data`)?n.data:void 0}))}}),K.forEach([`post`,`put`,`patch`,`query`],function(e){function t(t){return function(n,r,i){return this.request(X(i||{},{method:e,headers:t?{"Content-Type":`multipart/form-data`}:{},url:n,data:r}))}}Q.prototype[e]=t(),e!==`query`&&(Q.prototype[e+`Form`]=t(!0))});var ci=class e{constructor(e){if(typeof e!=`function`)throw TypeError(`executor must be a function.`);let t;this.promise=new Promise(function(e){t=e});let n=this;this.promise.then(e=>{if(!n._listeners)return;let t=n._listeners.length;for(;t-->0;)n._listeners[t](e);n._listeners=null}),this.promise.then=e=>{let t,r=new Promise(e=>{n.subscribe(e),t=e}).then(e);return r.cancel=function(){n.unsubscribe(t)},r},e(function(e,r,i){n.reason||(n.reason=new $n(e,r,i),t(n.reason))})}throwIfRequested(){if(this.reason)throw this.reason}subscribe(e){if(this.reason){e(this.reason);return}this._listeners?this._listeners.push(e):this._listeners=[e]}unsubscribe(e){if(!this._listeners)return;let t=this._listeners.indexOf(e);t!==-1&&this._listeners.splice(t,1)}toAbortSignal(){let e=new AbortController,t=t=>{e.abort(t)};return this.subscribe(t),e.signal.unsubscribe=()=>this.unsubscribe(t),e.signal}static source(){let t;return{token:new e(function(e){t=e}),cancel:t}}};function li(e){return function(t){return e.apply(null,t)}}function ui(e){return K.isObject(e)&&e.isAxiosError===!0}var di={Continue:100,SwitchingProtocols:101,Processing:102,EarlyHints:103,Ok:200,Created:201,Accepted:202,NonAuthoritativeInformation:203,NoContent:204,ResetContent:205,PartialContent:206,MultiStatus:207,AlreadyReported:208,ImUsed:226,MultipleChoices:300,MovedPermanently:301,Found:302,SeeOther:303,NotModified:304,UseProxy:305,Unused:306,TemporaryRedirect:307,PermanentRedirect:308,BadRequest:400,Unauthorized:401,PaymentRequired:402,Forbidden:403,NotFound:404,MethodNotAllowed:405,NotAcceptable:406,ProxyAuthenticationRequired:407,RequestTimeout:408,Conflict:409,Gone:410,LengthRequired:411,PreconditionFailed:412,PayloadTooLarge:413,UriTooLong:414,UnsupportedMediaType:415,RangeNotSatisfiable:416,ExpectationFailed:417,ImATeapot:418,MisdirectedRequest:421,UnprocessableEntity:422,Locked:423,FailedDependency:424,TooEarly:425,UpgradeRequired:426,PreconditionRequired:428,TooManyRequests:429,RequestHeaderFieldsTooLarge:431,UnavailableForLegalReasons:451,InternalServerError:500,NotImplemented:501,BadGateway:502,ServiceUnavailable:503,GatewayTimeout:504,HttpVersionNotSupported:505,VariantAlsoNegotiates:506,InsufficientStorage:507,LoopDetected:508,NotExtended:510,NetworkAuthenticationRequired:511,WebServerReturnsAnUnknownError:520,WebServerIsDown:521,ConnectionTimedOut:522,OriginIsUnreachable:523,TimeoutOccurred:524,SslHandshakeFailed:525,InvalidSslCertificate:526};Object.entries(di).forEach(([e,t])=>{di[t]=e});function fi(e){let t=new Q(e),n=ze(Q.prototype.request,t);return K.extend(n,Q.prototype,t,{allOwnKeys:!0}),K.extend(n,t,null,{allOwnKeys:!0}),n.create=function(t){return fi(X(e,t))},n}var $=fi(Xn);$.Axios=Q,$.CanceledError=$n,$.CancelToken=ci,$.isCancel=Qn,$.VERSION=Vr,$.toFormData=Dn,$.AxiosError=J,$.Cancel=$.CanceledError,$.all=function(e){return Promise.all(e)},$.spread=li,$.isAxiosError=ui,$.mergeConfig=X,$.AxiosHeaders=q,$.formToJSON=e=>qn(K.isHTMLForm(e)?new FormData(e):e),$.getAdapter=ti.getAdapter,$.HttpStatusCode=di,$.default=$;function pi(){try{let e=window.localStorage.getItem(Re.deviceId);return e||(e=mi(),window.localStorage.setItem(Re.deviceId,e)),e}catch{return mi()}}function mi(){return typeof crypto<`u`&&typeof crypto.randomUUID==`function`?crypto.randomUUID():`xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx`.replace(/[xy]/g,e=>{let t=Math.random()*16|0;return(e===`x`?t:t&3|8).toString(16)})}var hi=Pe()(Le(e=>({token:null,refreshToken:null,deviceId:pi(),user:null,status:`idle`,setToken:t=>e({token:t}),setUser:t=>e({user:t}),setStatus:t=>e({status:t}),setAuth:(t,n)=>e({token:t,user:n,status:`authenticated`}),setAuthSession:(t,n,r)=>e({token:t,refreshToken:n,user:r,status:`authenticated`}),setTokenPair:(t,n)=>e({token:t,refreshToken:n}),logout:()=>e({token:null,refreshToken:null,user:null,status:`unauthenticated`})}),{name:Re.auth,partialize:e=>({token:e.token,refreshToken:e.refreshToken,user:e.user}),onRehydrateStorage:()=>e=>{e?.token&&e.setStatus(`authenticated`)}})),gi=e=>!!e.token;function _i(e,t=`Something went wrong. Please try again.`){if($.isAxiosError(e)){if(e.code===`ERR_NETWORK`)return`Unable to reach the server. Please check your connection.`;let t=e.response?.data;if(t?.message)return t.message;if(Array.isArray(t?.errors)&&t.errors.length>0)return t.errors.join(`, `);if(e.message)return e.message}return e instanceof Error&&e.message?e.message:t}var vi=$.create({baseURL:i,timeout:a,headers:{"Content-Type":`application/json`}});vi.interceptors.request.use(e=>{let{token:t,deviceId:n}=hi.getState();return t&&(e.headers.Authorization=`Bearer ${t}`),e.headers[`X-Device-Id`]=n,e});var yi=null;async function bi(){let e=hi.getState().refreshToken;if(!e)return null;try{let{data:t}=await $.post(`${i}/auth/refresh`,{refreshToken:e},{headers:{"Content-Type":`application/json`}}),n=t.data;return hi.getState().setTokenPair(n.token,n.refreshToken??null),n.token}catch{return hi.getState().logout(),null}}function xi(e){hi.getState().logout(),window.location.pathname!==t.login&&(R.error(e),window.location.href=t.login)}vi.interceptors.response.use(e=>e,async e=>{let{response:t,config:n}=e,r=n?.skipAuthRedirect??!1,i=n?.silent??!1,a=_i(e);if(!t)return i||R.error(`Unable to reach the server. Please check your connection.`),Promise.reject(e);if(t.status===401&&!r&&n){let t=String(n.url??``).includes(`/auth/refresh`),r=n._retried===!0;if(!t&&!r){yi??=bi();let e=await yi;if(yi=null,e)return vi({...n,_retried:!0,headers:{...n.headers,Authorization:`Bearer ${e}`}})}return xi(a||`Your session has expired. Please sign in again.`),Promise.reject(e)}switch(t.status){case 403:i||R.error(a||`You do not have permission to perform this action.`);break;case 500:i||R.error(a||`Something went wrong on our end. Please try again.`)}return Promise.reject(e)});var Si={auth:{login:`/auth/login`,verifyLogin:`/auth/login/verify`,register:`/auth/register`,verifyRegistration:`/auth/register/verify`,resendOtp:`/auth/otp/resend`,forgotPassword:`/auth/forgot-password`,verifyForgotPassword:`/auth/forgot-password/verify`,resetPassword:`/auth/forgot-password/reset`,refresh:`/auth/refresh`,logout:`/auth/logout`,logoutAll:`/auth/logout-all`,changePassword:`/auth/change-password`,sessions:`/auth/sessions`,session:e=>`/auth/sessions/${e}`,trustedDevices:`/auth/trusted-devices`,trustedDevice:e=>`/auth/trusted-devices/${e}`,loginHistory:`/auth/login-history`,securityLogs:`/auth/security-logs`,securityOverview:`/auth/security-overview`},users:{profile:`/users/me`},folders:{list:`/folders`,create:`/folders`,root:`/folders/root`,detail:e=>`/folders/${e}`,children:e=>`/folders/${e}/children`,rename:e=>`/folders/${e}`,remove:e=>`/folders/${e}`,trash:`/folders/trash`,restore:e=>`/folders/${e}/restore`,permanentRemove:e=>`/folders/${e}/permanent`},files:{list:`/files`,favorites:`/files/favorites`,upload:`/files/upload`,search:`/files/search`,detail:e=>`/files/${e}`,download:e=>`/files/${e}/download`,preview:e=>`/files/${e}/preview`,rename:e=>`/files/${e}`,move:e=>`/files/${e}/move`,favorite:e=>`/files/${e}/favorite`,remove:e=>`/files/${e}`,restore:e=>`/files/${e}/restore`,trash:`/files/trash`,permanentRemove:e=>`/files/${e}/permanent`,versions:e=>`/files/${e}/versions`,version:(e,t)=>`/files/${e}/versions/${t}`,restoreVersion:(e,t)=>`/files/${e}/versions/${t}/restore`,downloadVersion:(e,t)=>`/files/${e}/versions/${t}/download`,downloadZip:`/files/download-zip`,analytics:`/files/stats/overview`,auditLogs:`/files/audit-logs`,scanStatus:e=>`/files/${e}/scan-status`},share:{myShares:`/shares/my-shares`,sharedWithMe:`/shares/shared-with-me`,create:e=>`/shares/file/${e}`,remove:e=>`/shares/${e}`,update:e=>`/shares/${e}`,analytics:e=>`/shares/${e}/analytics`,public:e=>`/shares/public/${e}`,verifyPassword:e=>`/shares/public/${e}/verify-password`,download:e=>`/shares/public/${e}/download`,preview:e=>`/shares/public/${e}/preview`},notifications:{list:`/notifications`,unreadCount:`/notifications/unread-count`,markAsRead:e=>`/notifications/${e}/read`,markAllAsRead:`/notifications/read-all`,remove:e=>`/notifications/${e}`},admin:{systemHealth:`/admin/system/health`,userSummary:`/users/admin/summary`,users:`/users/admin`,storageOverview:`/files/admin/storage-overview`,auditLogs:`/files/admin/audit-logs`,minioStatus:`/files/admin/minio-status`,securityOverview:`/auth/admin/security-overview`,loginHistory:`/auth/admin/login-history`,securityLogs:`/auth/admin/security-logs`,setUserEnabled:e=>`/auth/admin/users/${e}/enabled`,setUserRole:e=>`/auth/admin/users/${e}/role`}};export{hi as a,Le as c,R as d,gi as i,Pe as l,vi as n,$ as o,_i as r,Re as s,Si as t,Oe as u};