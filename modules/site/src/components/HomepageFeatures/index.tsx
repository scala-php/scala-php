import clsx from "clsx";
import Heading from "@theme/Heading";
import styles from "./styles.module.css";

type FeatureItem = {
  title: JSX.Element;
  Svg: React.ComponentType<React.ComponentProps<"svg">>;
  description: JSX.Element;
};

const FeatureList: FeatureItem[] = [
  {
    /* love */
    title: <>Your favorite language... transpiled from Scala!</>,
    Svg: require("@site/static/img/undraw_showing_support_re_5f2v.svg").default,
    description: (
      <>
        Scala.php lets you use the features of Scala to build applications in
        the language you love, PHP.
      </>
    ),
  },
  {
    /* collaboration,aliens,  */
    title: <>Seamless interoperability with PHP</>,
    Svg: require("@site/static/img/undraw_connection_re_lcud.svg").default,
    description: (
      <>
        You can mix Scala and PHP syntax in the same project, allowing you to
        benefit from the best of both worlds.
      </>
    ),
  },
  {
    /* sharing, giving, copying, new life */
    title: <>Reuse Scala Libraries</>,
    Svg: require("@site/static/img/undraw_building_blocks_re_5ahy.svg").default,
    description: (
      <>
        Just like Scala.js and Scala Native, Scala.php allows you to use the
        entire ecosystem of Scala libraries - as long as they publish
        Scala.php-compatible artifacts.
      </>
    ),
  },
];

function Feature({ title, Svg, description }: FeatureItem) {
  return (
    <div className={clsx("col col--4")}>
      <div className="text--center">
        <Svg className={styles.featureSvg} role="img" />
      </div>
      <div className="text--center padding-horiz--md">
        <Heading as="h3">{title}</Heading>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures(): JSX.Element {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
