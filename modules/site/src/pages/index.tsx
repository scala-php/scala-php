import clsx from "clsx";
import Link from "@docusaurus/Link";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import Layout from "@theme/Layout";
import HomepageFeatures from "@site/src/components/HomepageFeatures";
import Heading from "@theme/Heading";
import CodeBlock from "@theme/CodeBlock";

import styles from "./index.module.css";

function HomepageHeader() {
  const { siteConfig } = useDocusaurusContext();
  return (
    <header className={clsx("hero hero--primary", styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className="hero__title">
          {siteConfig.title}
        </Heading>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to="/docs/getting-started"
          >
            Get started
          </Link>
        </div>
      </div>
    </header>
  );
}

const Teaser = () => {
  return (
    <section className="container" style={{ marginBottom: "40px" }}>
      <div>
        <Heading as="h2">
          Make some <code>$</code>, fast.{" "}
        </Heading>
        <p>
          With Scala.php, you get <code>$</code> <b>for free</b>, any time you
          define a variable!
        </p>
        <CodeBlock title="scala" language="scala">
          {`val greeting = "hello"`}
        </CodeBlock>
        <CodeBlock title="php" language="php">
          {`$greeting = "hello"`}
        </CodeBlock>
      </div>
      <div style={{ marginTop: "50px" }}>
        <Heading as="h2">
          <code>explode</code> strings like it's 2000.
        </Heading>
        <p>
          Scala.php's <b>seamless interop</b> allows you to easily call PHP
          functions from Scala:
        </p>
        <CodeBlock title="scala" language="scala">
          {`@php.native def explode(delimiter: String, text: String): Array[String] = php.native

explode(" ", "Hello world!") // Array("Hello", "world!")`}
        </CodeBlock>
      </div>
      <div style={{ marginTop: "50px" }}>
        <Heading as="h2">Migrate to PHP, one step at a time.</Heading>
        <p>
          Interop works both ways - write your new code in PHP and reuse{" "}
          <b>your old Scala</b> at the generous price of zero!
        </p>
        <CodeBlock title="legacy.scala" language="scala">
          {`@php.exported def boringFunctionalCode(i: Int) = i + 1`}
        </CodeBlock>
      </div>
      <CodeBlock title="modern.php" language="php">
        {`<?php
require "legacy.php";

// now we're talking!
echo boringFunctionalCode(42);`}
      </CodeBlock>
      <Link
        className="button button--primary button--lg"
        to="/docs/getting-started"
        style={{
          marginTop: "20px",
        }}
      >
        Get started now!
      </Link>
    </section>
  );
};

export default function Home(): JSX.Element {
  const { siteConfig } = useDocusaurusContext();

  return (
    <Layout title={`Hello from ${siteConfig.title}`}>
      <HomepageHeader />
      <main>
        <HomepageFeatures />
      </main>
      <Teaser />
    </Layout>
  );
}
