"use strict";(self.webpackChunksite_new=self.webpackChunksite_new||[]).push([[976],{676:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>u,contentTitle:()=>o,default:()=>h,frontMatter:()=>i,metadata:()=>c,toc:()=>d});var a=n(7624),r=n(2172),l=n(1268),s=n(5388);const i={title:"Getting started",sidebar_position:1},o=void 0,c={id:"getting-started",title:"Getting started",description:"Quick installation",source:"@site/docs/getting-started.mdx",sourceDirName:".",slug:"/getting-started",permalink:"/docs/getting-started",draft:!1,unlisted:!1,editUrl:"https://github.com/scala-php/scala-php/tree/main/modules/site/docs/getting-started.mdx",tags:[],version:"current",sidebarPosition:1,frontMatter:{title:"Getting started",sidebar_position:1},sidebar:"tutorialSidebar",next:{title:"Examples",permalink:"/docs/examples"}},u={},d=[{value:"Quick installation",id:"quick-installation",level:2},{value:"Installation (less quick)",id:"installation-less-quick",level:2},{value:"Usage",id:"usage",level:2}];function p(e){const t={a:"a",admonition:"admonition",code:"code",h2:"h2",p:"p",pre:"pre",...(0,r.M)(),...e.components};return(0,a.jsxs)(a.Fragment,{children:[(0,a.jsx)(t.h2,{id:"quick-installation",children:"Quick installation"}),"\n",(0,a.jsx)(t.p,{children:"We provide a Giter8 template, which you can apply to create a new project with Scala.php already set up."}),"\n",(0,a.jsx)(t.pre,{children:(0,a.jsx)(t.code,{className:"language-bash",children:"sbt new scala-php/scala-php.g8\n"})}),"\n",(0,a.jsx)(t.h2,{id:"installation-less-quick",children:"Installation (less quick)"}),"\n",(0,a.jsx)(t.admonition,{type:"info",children:(0,a.jsx)(t.p,{children:"Scala.php only supports Scala 3.3.0 and above."})}),"\n",(0,a.jsxs)(t.admonition,{type:"warning",children:[(0,a.jsx)(t.p,{children:"Scala.php is still deep in development. Many features of the Scala language currently fail to compile."}),(0,a.jsx)(t.p,{children:"Adjust your expectations accordingly."})]}),"\n",(0,a.jsxs)(l.c,{groupId:"build-tool",children:[(0,a.jsxs)(s.c,{value:"sbt",children:[(0,a.jsxs)(t.p,{children:["In your ",(0,a.jsx)(t.code,{children:"project/plugins.sbt"}),":"]}),(0,a.jsx)(t.pre,{children:(0,a.jsx)(t.code,{className:"language-scala",metastring:'title="project/plugins.sbt" copy',children:'addSbtPlugin("org.scala-php" % "scala-php-sbt" % "0.1.1")\n'})}),(0,a.jsx)(t.p,{children:"Now, you can enable the plugin on the modules you want to compile to PHP:"}),(0,a.jsx)(t.pre,{children:(0,a.jsx)(t.code,{className:"language-scala",metastring:'title="build.sbt"',children:'val main = project\n  .settings(scalaVersion := "3.4.0")\n  .enablePlugins(PhpPlugin)\n'})})]}),(0,a.jsx)(s.c,{value:"Mill (coming soon)",children:(0,a.jsx)(t.p,{children:"Coming Soon\u2122"})})]}),"\n",(0,a.jsx)(t.h2,{id:"usage",children:"Usage"}),"\n",(0,a.jsx)(t.p,{children:"In order to start building with Scala.php, simply create a Scala file in the source root of your project and start writing Scala code:"}),"\n",(0,a.jsxs)(l.c,{groupId:"build-tool",children:[(0,a.jsx)(s.c,{value:"sbt",children:(0,a.jsx)(t.pre,{children:(0,a.jsx)(t.code,{className:"language-scala",metastring:'title="src/main/scala/Main.scala"',children:'object HelloWorld {\n\n  def main(args: Array[String]): Unit = {\n    val greeting = "hello"\n    println(s"$greeting world!")\n  }\n\n}\n'})})}),(0,a.jsx)(s.c,{value:"Mill (coming soon)",children:(0,a.jsx)(t.p,{children:"Coming Soon\u2122"})})]}),"\n",(0,a.jsx)(t.p,{children:"Now, you can compile and run the code with the PHP interpreter."}),"\n",(0,a.jsx)(t.admonition,{type:"info",children:(0,a.jsxs)(t.p,{children:["You must have a PHP interpreter available. By default, the PATH is used. To specify a custom interpreter, see ",(0,a.jsx)(t.a,{href:"/docs/configuration#php-interpreter",children:"Configuration"}),"."]})}),"\n",(0,a.jsxs)(l.c,{groupId:"build-tool",children:[(0,a.jsx)(s.c,{value:"sbt",children:(0,a.jsx)(t.pre,{children:(0,a.jsx)(t.code,{className:"language-shell",children:"sbt run\n"})})}),(0,a.jsx)(s.c,{value:"Mill (coming soon)",children:(0,a.jsx)(t.p,{children:"Coming Soon\u2122"})})]}),"\n",(0,a.jsx)(t.pre,{children:(0,a.jsx)(t.code,{className:"language-plaintext",metastring:'title="Output"',children:"hello world!\n"})}),"\n",(0,a.jsxs)(t.p,{children:["Check out the ",(0,a.jsx)(t.a,{href:"/docs/examples",children:"examples"})," for more complex use cases."]})]})}function h(e={}){const{wrapper:t}={...(0,r.M)(),...e.components};return t?(0,a.jsx)(t,{...e,children:(0,a.jsx)(p,{...e})}):p(e)}},5388:(e,t,n)=>{n.d(t,{c:()=>s});n(1504);var a=n(5456);const r={tabItem:"tabItem_Ymn6"};var l=n(7624);function s(e){let{children:t,hidden:n,className:s}=e;return(0,l.jsx)("div",{role:"tabpanel",className:(0,a.c)(r.tabItem,s),hidden:n,children:t})}},1268:(e,t,n)=>{n.d(t,{c:()=>w});var a=n(1504),r=n(5456),l=n(3943),s=n(5592),i=n(5288),o=n(632),c=n(7128),u=n(1148);function d(e){return a.Children.toArray(e).filter((e=>"\n"!==e)).map((e=>{if(!e||(0,a.isValidElement)(e)&&function(e){const{props:t}=e;return!!t&&"object"==typeof t&&"value"in t}(e))return e;throw new Error(`Docusaurus error: Bad <Tabs> child <${"string"==typeof e.type?e.type:e.type.name}>: all children of the <Tabs> component should be <TabItem>, and every <TabItem> should have a unique "value" prop.`)}))?.filter(Boolean)??[]}function p(e){const{values:t,children:n}=e;return(0,a.useMemo)((()=>{const e=t??function(e){return d(e).map((e=>{let{props:{value:t,label:n,attributes:a,default:r}}=e;return{value:t,label:n,attributes:a,default:r}}))}(n);return function(e){const t=(0,c.w)(e,((e,t)=>e.value===t.value));if(t.length>0)throw new Error(`Docusaurus error: Duplicate values "${t.map((e=>e.value)).join(", ")}" found in <Tabs>. Every value needs to be unique.`)}(e),e}),[t,n])}function h(e){let{value:t,tabValues:n}=e;return n.some((e=>e.value===t))}function m(e){let{queryString:t=!1,groupId:n}=e;const r=(0,s.Uz)(),l=function(e){let{queryString:t=!1,groupId:n}=e;if("string"==typeof t)return t;if(!1===t)return null;if(!0===t&&!n)throw new Error('Docusaurus error: The <Tabs> component groupId prop is required if queryString=true, because this value is used as the search param name. You can also provide an explicit value such as queryString="my-search-param".');return n??null}({queryString:t,groupId:n});return[(0,o._M)(l),(0,a.useCallback)((e=>{if(!l)return;const t=new URLSearchParams(r.location.search);t.set(l,e),r.replace({...r.location,search:t.toString()})}),[l,r])]}function g(e){const{defaultValue:t,queryString:n=!1,groupId:r}=e,l=p(e),[s,o]=(0,a.useState)((()=>function(e){let{defaultValue:t,tabValues:n}=e;if(0===n.length)throw new Error("Docusaurus error: the <Tabs> component requires at least one <TabItem> children component");if(t){if(!h({value:t,tabValues:n}))throw new Error(`Docusaurus error: The <Tabs> has a defaultValue "${t}" but none of its children has the corresponding value. Available values are: ${n.map((e=>e.value)).join(", ")}. If you intend to show no default tab, use defaultValue={null} instead.`);return t}const a=n.find((e=>e.default))??n[0];if(!a)throw new Error("Unexpected error: 0 tabValues");return a.value}({defaultValue:t,tabValues:l}))),[c,d]=m({queryString:n,groupId:r}),[g,b]=function(e){let{groupId:t}=e;const n=function(e){return e?`docusaurus.tab.${e}`:null}(t),[r,l]=(0,u.IN)(n);return[r,(0,a.useCallback)((e=>{n&&l.set(e)}),[n,l])]}({groupId:r}),f=(()=>{const e=c??g;return h({value:e,tabValues:l})?e:null})();(0,i.c)((()=>{f&&o(f)}),[f]);return{selectedValue:s,selectValue:(0,a.useCallback)((e=>{if(!h({value:e,tabValues:l}))throw new Error(`Can't select invalid tab value=${e}`);o(e),d(e),b(e)}),[d,b,l]),tabValues:l}}var b=n(3664);const f={tabList:"tabList__CuJ",tabItem:"tabItem_LNqP"};var x=n(7624);function v(e){let{className:t,block:n,selectedValue:a,selectValue:s,tabValues:i}=e;const o=[],{blockElementScrollPositionUntilNextRender:c}=(0,l.MV)(),u=e=>{const t=e.currentTarget,n=o.indexOf(t),r=i[n].value;r!==a&&(c(t),s(r))},d=e=>{let t=null;switch(e.key){case"Enter":u(e);break;case"ArrowRight":{const n=o.indexOf(e.currentTarget)+1;t=o[n]??o[0];break}case"ArrowLeft":{const n=o.indexOf(e.currentTarget)-1;t=o[n]??o[o.length-1];break}}t?.focus()};return(0,x.jsx)("ul",{role:"tablist","aria-orientation":"horizontal",className:(0,r.c)("tabs",{"tabs--block":n},t),children:i.map((e=>{let{value:t,label:n,attributes:l}=e;return(0,x.jsx)("li",{role:"tab",tabIndex:a===t?0:-1,"aria-selected":a===t,ref:e=>o.push(e),onKeyDown:d,onClick:u,...l,className:(0,r.c)("tabs__item",f.tabItem,l?.className,{"tabs__item--active":a===t}),children:n??t},t)}))})}function j(e){let{lazy:t,children:n,selectedValue:r}=e;const l=(Array.isArray(n)?n:[n]).filter(Boolean);if(t){const e=l.find((e=>e.props.value===r));return e?(0,a.cloneElement)(e,{className:"margin-top--md"}):null}return(0,x.jsx)("div",{className:"margin-top--md",children:l.map(((e,t)=>(0,a.cloneElement)(e,{key:t,hidden:e.props.value!==r})))})}function y(e){const t=g(e);return(0,x.jsxs)("div",{className:(0,r.c)("tabs-container",f.tabList),children:[(0,x.jsx)(v,{...e,...t}),(0,x.jsx)(j,{...e,...t})]})}function w(e){const t=(0,b.c)();return(0,x.jsx)(y,{...e,children:d(e.children)},String(t))}},2172:(e,t,n)=>{n.d(t,{I:()=>i,M:()=>s});var a=n(1504);const r={},l=a.createContext(r);function s(e){const t=a.useContext(l);return a.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function i(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(r):e.components||r:s(e.components),a.createElement(l.Provider,{value:t},e.children)}}}]);