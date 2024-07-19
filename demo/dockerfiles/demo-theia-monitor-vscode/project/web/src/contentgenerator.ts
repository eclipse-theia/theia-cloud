interface Config {
  name: string;
  time: Date;
}

function generateContents(config: Config) {
  return (
    "Hello " +
    config.name +
    "! It's " +
    config.time.getHours() +
    " o'clock and " +
    config.time.getMinutes() +
    " minutes."
  );
}

var config: Config = {
  name: "Sasha",
  time: new Date(),
};

document.body.innerHTML = generateContents(config);
