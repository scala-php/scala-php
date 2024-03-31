import clsx from "clsx";
import Link from "@docusaurus/Link";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import Layout from "@theme/Layout";
import HomepageFeatures from "@site/src/components/HomepageFeatures";
import Heading from "@theme/Heading";
import CodeBlock from "@theme/CodeBlock";
import styles from "./index.module.css";
import Logo from "../../static/img/logo.svg";

function HomepageHeader() {
  const { siteConfig } = useDocusaurusContext();
  return (
    <header className={clsx("hero hero--primary", styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className={"hero__title " + styles.heroTitle}>
          <Logo className={styles.logo} />
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

const Upcoming = () => (
  <span className={styles.upcoming}>
    <span className={styles.upcomingText}>upcoming</span>
  </span>
);

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
          Scala.php's <Link to="/docs/interop">seamless interop</Link> allows
          you to easily call PHP functions from Scala:
        </p>
        <CodeBlock title="scala" language="scala">
          {`@php.native def explode(separator: String, string: String): Array[String] = php.native

explode(" ", "Hello world!") // Array("Hello", "world!")`}
        </CodeBlock>
      </div>
      <div style={{ marginTop: "50px" }}>
        <Heading as="h2">
          <Upcoming />
          Migrate to PHP, one step at a time.
        </Heading>
        <p>
          <Link to="/docs/interop">Interop</Link> works both ways - write your
          new code in PHP and reuse <b>your old Scala</b> at the generous price
          of zero!
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

const Testimonials = () => {
  return (
    <section className="container" style={{ marginBottom: "40px" }}>
      <Heading as="h2">Testimonials</Heading>
      <p>See what the early users have to say about Scala.php:</p>
      <div className={styles.testimonials}>
        <div className={styles.testimonial}>
          <q>
            Ay yo, you know, I gotta give mad props to Scala.php, cuz it's like
            taking the best of both worlds and mixin' 'em up like a bomb-ass
            cocktail. It's like, Scala's got that smooth vibe, and PHP's got
            that hustle, put 'em together and you got yourself a party.
          </q>
          <p>
            <b>Snoop Dogg</b>, genius, billionaire, playboy, philanthropist
          </p>
        </div>
        <hr />
        <div className={styles.testimonial}>
          <q>
            I was skeptical at first, but after trying Scala.php, I was
            convinced. It's the best thing since sliced bread!
          </q>
          <p>
            <b>Jane Doe</b>, former baker
          </p>
        </div>
        <hr />
        <div className={styles.testimonial}>
          <q>You're a sick guy, you know that?</q>
          <p>
            <b>Josh Long</b>, PHP developer advocate
          </p>
        </div>
        <hr />
        <div className={styles.testimonial}>
          <q>This shit wasn't part of the job description</q>
          <p>
            <b>Łukasz Biały</b>, Scala developer advocate
          </p>
        </div>
        <hr />
        <div className={styles.testimonial}>
          <q>
            You're telling me I could've been doing this instead of building
            keyboards?
          </q>
          <p>
            <b>Kasper Kondzielski</b>, Neovim enjoyer
          </p>
        </div>
      </div>
    </section>
  );
};
export default function Home(): JSX.Element {
  const { siteConfig } = useDocusaurusContext();

  return (
    <Layout title={`Welcome to the world of ${siteConfig.title}!`}>
      <HomepageHeader />
      <main>
        <HomepageFeatures />
      </main>
      <Teaser />
      <Testimonials />
    </Layout>
  );
}
